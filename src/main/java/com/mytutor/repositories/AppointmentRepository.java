package com.mytutor.repositories;

import com.mytutor.constants.AppointmentStatus;
import com.mytutor.dto.statistics.DateTuitionSum;
import com.mytutor.dto.statistics.StudentProfitDto;
import com.mytutor.dto.statistics.SubjectTuitionSum;
import com.mytutor.dto.statistics.TutorIncomeDto;
import com.mytutor.entities.Account;
import com.mytutor.entities.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @author vothimaihoa
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    @Query("SELECT a FROM Appointment a " +
            " WHERE (a.tutor.id = :accountId OR a.student.id = :accountId)" +
            " AND (:status is null OR a.status = :status)" +
            " ORDER BY a.createdAt DESC ")
    Page<Appointment> findAppointmentByAccountId(Integer accountId, AppointmentStatus status, Pageable pageable);

    @Query("SELECT a FROM Appointment a "
            + " WHERE :status is null OR a.status = :status" +
            "  ORDER BY a.createdAt DESC ")
    Page<Appointment> findAppointments(AppointmentStatus status, Pageable pageable);

    @Query("SELECT DISTINCT a.tutor FROM Appointment a WHERE a.student.id = :studentId AND a.status = :status")
    List<Account> findAllBookedTutorsByStudentIdAndStatus(@Param("studentId") int studentId, @Param("status") AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.status = :status AND a.student.id = :studentId")
    List<Appointment> findAppointmentsWithPendingPayment(@Param("studentId") Integer studentId,
                                                         @Param("status") AppointmentStatus status);

    boolean existsByTutorIdAndStudentIdAndStatus(Integer tutorId, Integer studentId, AppointmentStatus status);

    // rollback automatically after 30 minutes
    List<Appointment> findByStatusAndCreatedAtBefore(AppointmentStatus status, LocalDateTime dateTime);

    @Query("SELECT a FROM Appointment a " +
            " WHERE (a.student.id = :id) AND " +
            " (:startDate IS NULL OR a.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR a.createdAt < :endDate)")
    List<Appointment> findAppointmentsInTimeRangeByStudent(@Param("id") Integer id,
                                    @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM Appointment a " +
            " WHERE (a.tutor.id = :id) AND " +
            " (:startDate IS NULL OR a.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR a.createdAt < :endDate)")
    List<Appointment> findAppointmentsInTimeRangeByTutor(@Param("id") Integer id,
                                                           @Param("startDate") LocalDateTime startDate,
                                                           @Param("endDate") LocalDateTime endDate);



    @Query("SELECT new com.mytutor.dto.statistics.SubjectTuitionSum(s.subjectName, COALESCE(SUM(a.tuition), 0)) " +
            "FROM Subject s " +
            "LEFT JOIN Appointment a ON s.id = a.subject.id AND a.status = :status " +
            "GROUP BY s.subjectName")
    List<SubjectTuitionSum> findTotalTuitionBySubject(@Param("status") AppointmentStatus status);

    @Query("SELECT new com.mytutor.dto.statistics.DateTuitionSum(DATE(a.createdAt), SUM(a.tuition)) " +
            "FROM Appointment a " +
            "WHERE a.status = :status  " +
            "GROUP BY DATE(a.createdAt) " +
            "ORDER BY DATE(a.createdAt) ASC")
    List<DateTuitionSum> findTotalTuitionByDate(@Param("status") AppointmentStatus status);

    @Query("SELECT SUM(a.tuition) " +
            "FROM Appointment a " +
            "WHERE a.status = 'PAID' AND MONTH(a.createdAt) = MONTH(:date) AND YEAR(a.createdAt) = YEAR(:date)")
    Double getRevenue(@Param("date") Date date);

    @Query("SELECT SUM(a.tuition * td.percentage / 100) " +
            "FROM Appointment a " +
            "JOIN a.tutor t " +
            "JOIN t.tutorDetail td " +
            "WHERE a.status = 'PAID' AND MONTH(a.createdAt) = MONTH(:date) AND YEAR(a.createdAt) = YEAR(:date)")
    Double getProfit(@Param("date") Date date);

    List<Appointment> findByStatusOrderByCreatedAtDesc(AppointmentStatus status);

    @Query("SELECT new com.mytutor.dto.statistics.StudentProfitDto(" +
            "demo.studentId, acc.fullName, SUM(demo.tuition), SUM(demo.profit), COUNT(demo.appointmentId)) " +
            "FROM (" +
            "    SELECT " +
            "        ap.student.id AS studentId, " +
            "        ap.id AS appointmentId, " +
            "        SUM(ap.tuition) AS tuition, " +
            "        SUM(ap.tuition * td.percentage / 100) AS profit " +
            "    FROM Appointment ap " +
            "    JOIN ap.tutor acc " +
            "    JOIN acc.tutorDetail td " +
            "    WHERE ap.status = :status " +
            "    AND (:month IS NULL OR MONTH(ap.createdAt) = :month) " +
            "    AND (:year IS NULL OR YEAR(ap.createdAt) = :year) " +
            "    GROUP BY ap.student.id, td.id, ap.id" +
            ") demo " +
            "JOIN Account acc ON acc.id = demo.studentId " +
            "GROUP BY demo.studentId, acc.fullName")
    List<StudentProfitDto> findStudentProfits(@Param("status") AppointmentStatus status, @Param("month") Integer month, @Param("year") Integer year);

    @Query("SELECT new com.mytutor.dto.statistics.TutorIncomeDto(" +
            "ap.tutor.id, ac.fullName, " +
            "SUM(ap.tuition), td.percentage, " +
            "SUM(ap.tuition) * (100 - td.percentage) / 100, " +
            "SUM(ap.tuition) * td.percentage / 100, " +
            "COUNT(ap.id)) " +
            "FROM Appointment ap " +
            "JOIN ap.tutor ac " +
            "JOIN ac.tutorDetail td " +
            "WHERE ap.status = :status " +
            "AND (:month IS NULL OR MONTH(ap.createdAt) = :month) " +
            "AND (:year IS NULL OR YEAR(ap.createdAt) = :year) " +
            "GROUP BY ap.tutor.id, ac.fullName, td.percentage")
    List<TutorIncomeDto> findTutorIncomes(@Param("status") AppointmentStatus status, @Param("month") Integer month, @Param("year") Integer year);
}
