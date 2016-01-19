package org.hibernate.test.annotations.derivedidentities.e3.b2;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class EmployeeId implements Serializable {
    @Column(length = 80) // for some reason db2 complains about too large PK if this is set to default (255)
    String firstName;
    String lastName;
}
