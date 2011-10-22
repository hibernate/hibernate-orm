package org.hibernate.envers.test.integration.onetoone.bidirectional;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class AuditedRelOwnedEntity {
    @Id
    private Long id;

    private String data;

    @OneToOne(optional=false, mappedBy="reference")
    @Audited(targetAuditMode=RelationTargetAuditMode.NOT_AUDITED)
    private NotAuditedRelOwningEntity target;

    public AuditedRelOwnedEntity() {
    }

    public AuditedRelOwnedEntity(Long id, String data, NotAuditedRelOwningEntity target) {
        this.id = id;
        this.data = data;
        this.target = target;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditedRelOwnedEntity)) return false;

        AuditedRelOwnedEntity that = (AuditedRelOwnedEntity) o;

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
        return "AuditedRelOwnedEntity(id = " + id + ", data = " + data + ")";
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

    public NotAuditedRelOwningEntity getTarget() {
        return target;
    }

    public void setTarget(NotAuditedRelOwningEntity target) {
        this.target = target;
    }
}
