package co.za.payments.ledger.repository;

import co.za.payments.ledger.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    Optional<LedgerEntry> findFirstByTransferIdOrderByCreatedAtAsc(UUID transferId);

    List<LedgerEntry> findByTransferId(UUID transferId);

}
