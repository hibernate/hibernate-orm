package org.hibernate.test.lazyload;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = "1")
public class Child1Entity extends ParentManyEntity {

}
