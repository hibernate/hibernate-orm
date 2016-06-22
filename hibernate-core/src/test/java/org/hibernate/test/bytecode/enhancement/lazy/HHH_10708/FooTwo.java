package org.hibernate.test.bytecode.enhancement.lazy.HHH_10708;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
class FooTwo {

    @Id
    @GeneratedValue
    int id;
}
