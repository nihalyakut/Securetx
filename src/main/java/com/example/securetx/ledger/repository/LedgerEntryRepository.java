package com.example.securetx.ledger.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.securetx.ledger.entity.LedgerEntry;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

	@Query("""
			    select l from LedgerEntry l
			    where l.accountId = :accountId
			      and (:cursor is null or l.id < :cursor)
			    order by l.id desc
			""")
	List<LedgerEntry> findStatement(@Param("accountId") Long accountId, @Param("cursor") Long cursor, Pageable pageable);
}
