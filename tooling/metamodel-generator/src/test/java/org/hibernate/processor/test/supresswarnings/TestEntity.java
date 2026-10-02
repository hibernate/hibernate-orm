package org.hibernate.processor.test.supresswarnings;

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
