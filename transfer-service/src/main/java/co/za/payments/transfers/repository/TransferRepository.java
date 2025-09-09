package co.za.payments.transfers.repository;

import co.za.payments.transfers.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> { }
