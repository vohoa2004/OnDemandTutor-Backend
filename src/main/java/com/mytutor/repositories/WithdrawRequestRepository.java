package com.mytutor.repositories;

import com.mytutor.constants.WithdrawRequestStatus;
import com.mytutor.entities.Account;
import com.mytutor.entities.WithdrawRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawRequestRepository extends JpaRepository<WithdrawRequest, Integer> {

    // find by tutor, month, year, status
    WithdrawRequest findByTutorAndMonthAndYearAndStatus(Account tutor, int month, int year, WithdrawRequestStatus status);

    WithdrawRequest findTopByTutorAndMonthAndYearOrderByCreatedAtDesc(Account tutor, int month, int year);

    @Query("SELECT w from WithdrawRequest w order by w.status, w.createdAt")
    Page<WithdrawRequest> findAndSortByStatusAndTime(Pageable pageable);
}
