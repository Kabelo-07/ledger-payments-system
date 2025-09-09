package co.za.payments.ledger.domain;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.UUID;

@MappedSuperclass
public abstract class VersionedEntity extends AbstractEntity {

    @Version
    @Column(nullable = false)
    private int version;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
