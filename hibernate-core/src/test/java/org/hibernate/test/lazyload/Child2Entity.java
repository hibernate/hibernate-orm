package org.hibernate.test.lazyload;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = "2")
public class Child2Entity extends ParentManyEntity {

}
