package org.jboss.envers.test.integration.inheritance.single.childrelation;

import org.jboss.envers.Versioned;

import javax.persistence.*;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@DiscriminatorValue("2")
@Versioned
public class ChildIngEntity extends ParentNotIngEntity {
    @Basic
    private Long number;

    @ManyToOne
    private ReferencedEntity referenced;

    public ChildIngEntity() {
    }

    public ChildIngEntity(String data, Long number) {
        super(data);
        this.number = number;
    }

    public ChildIngEntity(Integer id, String data, Long number) {
        super(id, data);
        this.number = number;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public ReferencedEntity getReferenced() {
        return referenced;
    }

    public void setReferenced(ReferencedEntity referenced) {
        this.referenced = referenced;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChildIngEntity)) return false;
        if (!super.equals(o)) return false;

        ChildIngEntity childEntity = (ChildIngEntity) o;

        if (number != null ? !number.equals(childEntity.number) : childEntity.number != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (number != null ? number.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "ChildIngEntity(id = " + getId() + ", data = " + getData() + ", number = " + number + ")";
    }
}