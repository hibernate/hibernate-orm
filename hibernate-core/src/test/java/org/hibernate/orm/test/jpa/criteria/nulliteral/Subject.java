package org.hibernate.orm.test.jpa.criteria.nulliteral;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Andrea Boriero
 */
@Entity
public class Subject {
	@Id
	@GeneratedValue
	private long id;

	private String name;
}
