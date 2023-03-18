package org.hibernate.orm.test.tenantid;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Record {
    @Id @GeneratedValue
    public Long id;
    public State state = new State();
}
