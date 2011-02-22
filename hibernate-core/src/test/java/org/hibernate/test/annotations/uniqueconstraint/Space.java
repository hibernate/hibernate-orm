package org.hibernate.test.annotations.uniqueconstraint;

import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
@MappedSuperclass
@Table(uniqueConstraints = {@UniqueConstraint(name="inheritedConstraint", columnNames = {"matter", "value"})})
public class Space {

    private Matter matter;

    private Integer value;

    @ManyToOne
    public Matter getMatter() {
        return matter;
    }

    public void setMatter(Matter matter) {
        this.matter = matter;
    }

    @Column
    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
