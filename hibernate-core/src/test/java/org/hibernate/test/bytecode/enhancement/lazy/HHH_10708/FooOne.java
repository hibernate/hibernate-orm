package org.hibernate.test.bytecode.enhancement.lazy.HHH_10708;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
class FooOne {

    @Id
    @GeneratedValue
    int id;

    @OneToMany( orphanRemoval = true, mappedBy = BarOne.FOO, targetEntity = BarOne.class )
    @Cascade( CascadeType.ALL )
    Set<BarOne> bars = new HashSet<>();
}
