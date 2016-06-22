package org.hibernate.test.bytecode.enhancement.lazy.HHH_10708;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
class BarOne {
    static final String FOO = "foo";

    @Id
    @GeneratedValue
    int id;

    @ManyToOne
    @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
    FooOne foo;
}
