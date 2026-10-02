package org.hibernate.processor.test.mappedsuperclass.mappedsuperclasswithoutid;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Shop {
	@Id
	long id;
	String name;
}
