package org.hibernate.test.annotations.onetomany;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class E {
    @Id
    Long id;
}