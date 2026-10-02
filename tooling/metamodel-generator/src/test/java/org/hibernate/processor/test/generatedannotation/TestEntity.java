package org.hibernate.processor.test.generatedannotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class TestEntity {
	@Id
	private long id;
}
