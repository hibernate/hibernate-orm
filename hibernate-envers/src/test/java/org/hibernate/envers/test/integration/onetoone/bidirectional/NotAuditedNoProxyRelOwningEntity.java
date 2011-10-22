package org.hibernate.envers.test.integration.onetoone.bidirectional;

import org.hibernate.annotations.Proxy;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Proxy(lazy=false)
public class NotAuditedNoProxyRelOwningEntity {
    @Id
    private Long id;

    private String data;

    @OneToOne
    private AuditedNoProxyRelOwnedEntity reference;

    public NotAuditedNoProxyRelOwningEntity() {
    }

    public NotAuditedNoProxyRelOwningEntity(Long id, String data, AuditedNoProxyRelOwnedEntity reference) {
        this.id = id;
        this.data = data;
        this.reference = reference;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotAuditedNoProxyRelOwningEntity)) return false;

        NotAuditedNoProxyRelOwningEntity that = (NotAuditedNoProxyRelOwningEntity) o;

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
        return "NotAuditedNoProxyRelOwningEntity(id = " + id + ", data = " + data + ")";
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

    public AuditedNoProxyRelOwnedEntity getReference() {
        return reference;
    }

    public void setReference(AuditedNoProxyRelOwnedEntity reference) {
        this.reference = reference;
    }
}
