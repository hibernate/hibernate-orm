package org.hibernate.jpamodelgen.test.xmlonly;

import java.util.Set;

public class Car {
    public Long getId() {
        return 1L;
    }

    public void setId(Long id) {
    }

    public Set<Tire> getTires() {
        return null;
    }

    public void setTires(Set<Tire> tires) {
    }
}
