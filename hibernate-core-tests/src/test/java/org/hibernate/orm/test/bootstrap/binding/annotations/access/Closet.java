package org.hibernate.orm.test.bootstrap.binding.annotations.access;
import jakarta.persistence.Embeddable;

/**
 * @author Hardy Ferentschik
 */
@Embeddable
public class Closet extends BaseFurniture {
	int numberOfDoors;
}
