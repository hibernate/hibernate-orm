package org.hibernate.test.bytecode.enhancement.lazy.HHH_10708;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
class BarTwo {

    @Id
    @GeneratedValue
    int id;

    @ManyToMany( fetch = FetchType.LAZY, targetEntity = FooTwo.class )
    Set<FooTwo> foos = new HashSet<>();
}
