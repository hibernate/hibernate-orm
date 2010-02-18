package org.hibernate.envers.test.entities.ids;

import org.hibernate.envers.test.entities.UnversionedStrTestEntity;

import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;
import java.io.Serializable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Embeddable
public class ManyToOneNotAuditedEmbId implements Serializable {
    @ManyToOne
    private UnversionedStrTestEntity id;

    public ManyToOneNotAuditedEmbId() {
    }

    public ManyToOneNotAuditedEmbId(UnversionedStrTestEntity id) {
        this.id = id;
    }

    public UnversionedStrTestEntity getId() {
        return id;
    }

    public void setId(UnversionedStrTestEntity id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ManyToOneNotAuditedEmbId that = (ManyToOneNotAuditedEmbId) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
