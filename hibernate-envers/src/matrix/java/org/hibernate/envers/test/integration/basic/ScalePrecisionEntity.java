package org.hibernate.envers.test.integration.basic;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class ScalePrecisionEntity implements Serializable {
    @Id
    @GeneratedValue
    private Long id;

    @Column(precision = 3, scale = 0)
    private Double number;

    public ScalePrecisionEntity() {
    }

    public ScalePrecisionEntity(Double number) {
        this.number = number;
    }

    public ScalePrecisionEntity(Double number, Long id) {
        this.id = id;
        this.number = number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScalePrecisionEntity)) return false;

        ScalePrecisionEntity that = (ScalePrecisionEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (number != null ? !number.equals(that.number) : that.number != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (number != null ? number.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ScalePrecisionEntity(id = " + id + ", number = " + number + ")";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getNumber() {
        return number;
    }

    public void setNumber(Double number) {
        this.number = number;
    }
}
