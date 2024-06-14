package com.mytutor.repositories;

import com.mytutor.entities.Timeslot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 *
 * @author vothimaihoa
 */
@Repository
public interface TimeslotRepository extends JpaRepository<Timeslot, Integer> {

    @Query("SELECT t FROM Timeslot t " +
            "WHERE t.account.id = :tutorId " +
            "AND t.scheduleDate = :date " +
            "AND ((t.startTime >= :startTime AND t.startTime < :endTime) " +
            "OR (t.endTime > :startTime AND t.endTime <= :endTime) " +
            "OR (t.startTime <= :startTime AND t.endTime >= :endTime))")
    List<Timeslot> findOverlapTimeslot(@Param("tutorId") Integer tutorId,
                                 @Param("date") LocalDate date,
                                 @Param("startTime") LocalTime startTime,
                                 @Param("endTime") LocalTime endTime);


    @Query(" SELECT t FROM Timeslot t " +
            " WHERE t.account.id = :tutorId " +
            " AND t.scheduleDate BETWEEN :startDate AND :endDate " +
            " AND (t.startTime >= :startTime AND t.scheduleDate = :startDate) "+
            " AND t.isOccupied = false" +
            " AND t.dayOfWeek = :dayOfWeek " +
            " ORDER BY t.scheduleDate, t.startTime ASC")
    List<Timeslot> findByTutorIdAndDayOfWeekAndDateRange(@Param("startTime") LocalTime startTime,
                                                         @Param("tutorId") Integer tutorId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate,
                                                         @Param("dayOfWeek") Integer dayOfWeek);
}
