package org.hibernate.orm.test.annotations.inheritance.discriminatoroptions;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Hardy Ferentschik
 */
@Entity
@DiscriminatorValue("B")
public class SubClass2 extends BaseClass2 {
}
