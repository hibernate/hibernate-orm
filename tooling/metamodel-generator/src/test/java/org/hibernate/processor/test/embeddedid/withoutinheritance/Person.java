package org.hibernate.processor.test.embeddedid.withoutinheritance;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Person {
	@EmbeddedId
	private PersonId id;

	private String address;
}
