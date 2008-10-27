package org.jboss.envers.test.entities.customtype;

import org.jboss.envers.Versioned;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Columns;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@TypeDef(name = "comp", typeClass = CompositeTestUserType.class)
public class CompositeCustomTypeEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @Versioned
    @Type(type = "comp")
    @Columns(columns = { @Column(name = "str"), @Column(name = "num") })
    private Component component;

    public CompositeCustomTypeEntity() {
    }

    public CompositeCustomTypeEntity(Integer id, Component component) {
        this.id = id;
        this.component = component;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompositeCustomTypeEntity)) return false;

        CompositeCustomTypeEntity that = (CompositeCustomTypeEntity) o;

        if (component != null ? !component.equals(that.component) : that.component != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (component != null ? component.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "CCTE(id = " + id + ", component = " + component + ")";
    }
}
