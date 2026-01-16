package com.example.securetx.account.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.securetx.account.entity.Account;

import jakarta.persistence.LockModeType;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

	List<Account> findByUserId(Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			    select a
			    from Account a
			    where a.id = :id
			""")
	Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
