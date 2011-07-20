package org.hibernate.envers.test.integration.onetoone.bidirectional;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
public class NotAuditedRelOwningEntity {
    @Id
    private Long id;

    private String data;

    @OneToOne
    private AuditedRelOwnedEntity reference;

    public NotAuditedRelOwningEntity() {
    }

    public NotAuditedRelOwningEntity(Long id, String data, AuditedRelOwnedEntity reference) {
        this.id = id;
        this.data = data;
        this.reference = reference;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotAuditedRelOwningEntity)) return false;

        NotAuditedRelOwningEntity that = (NotAuditedRelOwningEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (data != null ? !data.equals(that.data) : that.data != null) return false;

        return true;
    }

    public int hashCode() {
        int result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "NotAuditedRelOwningEntity(id = " + id + ", data = " + data + ")";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public AuditedRelOwnedEntity getReference() {
        return reference;
    }

    public void setReference(AuditedRelOwnedEntity reference) {
        this.reference = reference;
    }
}
