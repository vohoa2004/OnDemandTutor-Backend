package com.mytutor.services.impl;

import com.mytutor.constants.AppointmentStatus;
import com.mytutor.dto.AppointmentDto;
import com.mytutor.dto.PaginationDto;
import com.mytutor.entities.Account;
import com.mytutor.entities.Appointment;
import com.mytutor.entities.Timeslot;
import com.mytutor.exceptions.*;
import com.mytutor.repositories.AccountRepository;
import com.mytutor.repositories.AppointmentRepository;
import com.mytutor.repositories.TimeslotRepository;
import com.mytutor.services.AppointmentService;
import com.mytutor.services.PaymentService;
import org.jetbrains.annotations.NotNull;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.sql.Time;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author vothimaihoa
 */
@Service
public class AppointmentServiceImpl implements AppointmentService {

    @Autowired
    AppointmentRepository appointmentRepository;

    @Autowired
    TimeslotRepository timeslotRepository;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public ResponseEntity<AppointmentDto> getAppointmentById(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found"));
        AppointmentDto dto = modelMapper.map(appointment, AppointmentDto.class);
        for (Timeslot t : appointment.getTimeslots()) {
            dto.getTimeslotIds().add(t.getId());
        }
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }

    @Override
    public ResponseEntity<PaginationDto<AppointmentDto>> getAppointmentsByTutorId(Integer tutorId,
                                                                                  AppointmentStatus status,
                                                                                  Integer pageNo, Integer pageSize) {
        return getPaginationDtoResponseEntity(tutorId, status, pageNo, pageSize);
    }

    @Override
    public ResponseEntity<PaginationDto<AppointmentDto>> getAppointmentsByStudentId(Integer studentId,
                                                                                    AppointmentStatus status,
                                                                                    Integer pageNo, Integer pageSize) {
        return getPaginationDtoResponseEntity(studentId, status, pageNo, pageSize);
    }

    @NotNull
    private ResponseEntity<PaginationDto<AppointmentDto>> getPaginationDtoResponseEntity(Integer accountId, AppointmentStatus status, Integer pageNo, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Appointment> appointments;
        if (status == null) {
            appointments = appointmentRepository.findAppointmentByTutorId(accountId, pageable);
        } else {
            appointments = appointmentRepository.findAppointmentByTutorId(accountId, status, pageable);
        }
        return ResponseEntity.status(HttpStatus.OK).body(getPaginationDto(appointments));
    }

    private PaginationDto<AppointmentDto> getPaginationDto(Page<Appointment> appointments) {
        List<Appointment> listOfAppointments = appointments.getContent();

        List<AppointmentDto> content = listOfAppointments.stream()
                .map(a -> {
                    Appointment appointment = appointmentRepository.findById(a.getId())
                            .orElse(new Appointment());
                    return modelMapper.map(appointment, AppointmentDto.class);
                })
                .collect(Collectors.toList());

        PaginationDto<AppointmentDto> appointmentResponseDto = new PaginationDto<>();
        appointmentResponseDto.setContent(content);
        appointmentResponseDto.setPageNo(appointments.getNumber());
        appointmentResponseDto.setPageSize(appointments.getSize());
        appointmentResponseDto.setTotalElements(appointments.getTotalElements());
        appointmentResponseDto.setTotalPages(appointments.getTotalPages());
        appointmentResponseDto.setLast(appointments.isLast());

        return appointmentResponseDto;
    }

    // student create appointment (not paid yet)
    @Override
    @Transactional
    public ResponseEntity<?> createAppointment(Integer studentId, AppointmentDto appointmentDto) {
        Account tutor = accountRepository.findById(appointmentDto.getTutorId())
                .orElseThrow(() -> new AccountNotFoundException("Tutor not found!"));

        if (!appointmentRepository.findAppointmentsWithPendingPayment(studentId,
                AppointmentStatus.PENDING_PAYMENT).isEmpty()) {
            throw new PaymentFailedException("This student is having another booking in pending payment status!");
        }
        appointmentDto.setCreatedAt(LocalDateTime.now());
        appointmentDto.setStatus(AppointmentStatus.PENDING_PAYMENT);
        Appointment appointment = modelMapper.map(appointmentDto, Appointment.class);
        for (Integer i : appointmentDto.getTimeslotIds()) {
            Timeslot t = timeslotRepository.findById(i).get();
            if (t.isOccupied()) {
                throw new ConflictTimeslotException("Cannot book because some timeslots are occupied!");
            }
            else {
                t.setOccupied(true);
                appointment.getTimeslots().add(t);
                t.setAppointment(appointment);
            }
        }


        appointment.setTuition(tutor.getTutorDetail().getTeachingPricePerHour()
                * calculateTotalHours(appointment.getTimeslots()));

        timeslotRepository.saveAll(appointment.getTimeslots());
        appointmentRepository.save(appointment);

        // response
        AppointmentDto dto = modelMapper.map(appointment, AppointmentDto.class);
        for (Timeslot t : appointment.getTimeslots()) {
            dto.getTimeslotIds().add(t.getId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    private double calculateTotalHours(List<Timeslot> timeslots) {
        double totalHours = 0;
        for (Timeslot t : timeslots) {
            LocalTime startLocalTime = t.getStartTime().toLocalTime();
            LocalTime endLocalTime = t.getEndTime().toLocalTime();
            Duration duration = Duration.between(startLocalTime, endLocalTime);
            totalHours += duration.toHours() + (duration.toMinutesPart() / 60.0);
        }
        return totalHours;
    }

    // tutor update appointment status: DONE from PAID or CANCELED from PAID
    @Override
    public ResponseEntity<?> updateAppointmentStatus(Integer tutorId, Integer appointmentId, String status) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found!"));

        if(!Objects.equals(tutorId, appointment.getTutor().getId())) {
            throw new AppointmentNotFoundException("This appointment is not belong to this tutor");
        }
        if (!appointment.getStatus().equals(AppointmentStatus.PAID)) {
            throw new InvalidAppointmentStatusException("Tutor can only update paid appointment!");
        }

        if (status.equals((AppointmentStatus.DONE).toString())) {
            // check dieu kien chua day xong ko cho DONE
            appointment.setStatus(AppointmentStatus.DONE);
        }

        else if (status.equalsIgnoreCase((AppointmentStatus.CANCELED).toString())) {
            appointment.setStatus(AppointmentStatus.CANCELED);
            // goi service hoan tien cho student...
        } else {
            throw new InvalidAppointmentStatusException("This status is invalid!");
        }

        appointmentRepository.save(appointment);

        return ResponseEntity.ok("Appointment status updated successfully");
    }

    // viết hàm rollback (xóa appointment + timeslot isOccupied = false + appointmentId = null)
    @Override
    public void rollbackAppointment(Appointment appointment) {
        for (Timeslot t : appointment.getTimeslots()) {
            t.setOccupied(false);
            t.setAppointment(null);
        }
        appointmentRepository.delete(appointment);
    }

    // student update appointment status (canceled)

    // sau khi hết 15p do vnpay đếm,
    // mọi thứ trong hàm create appointment sẽ bị roll back về trạng thái trước khi create appointment
    // ...



}
