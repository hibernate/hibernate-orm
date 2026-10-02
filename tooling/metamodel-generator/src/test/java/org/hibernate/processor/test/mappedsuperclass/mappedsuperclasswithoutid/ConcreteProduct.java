package org.hibernate.processor.test.mappedsuperclass.mappedsuperclasswithoutid;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class ConcreteProduct extends Product {
	@Id
	long id;
}
