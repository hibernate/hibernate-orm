package org.jboss.envers.test.integration.inheritance.single;

import org.jboss.envers.Versioned;

import javax.persistence.*;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@DiscriminatorValue("2")
@Versioned
public class ChildEntity extends ParentEntity {
    @Basic
    private Long number;

    public ChildEntity() {
    }

    public ChildEntity(String data, Long number) {
        super(data);
        this.number = number;
    }

    public ChildEntity(Integer id, String data, Long number) {
        super(id, data);
        this.number = number;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChildEntity)) return false;
        if (!super.equals(o)) return false;

        ChildEntity childEntity = (ChildEntity) o;

        if (number != null ? !number.equals(childEntity.number) : childEntity.number != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (number != null ? number.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "ChildEntity(id = " + getId() + ", data = " + getData() + ", number = " + number + ")";
    }
}