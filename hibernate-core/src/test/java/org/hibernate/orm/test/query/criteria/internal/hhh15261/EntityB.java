package org.hibernate.orm.test.query.criteria.internal.hhh15261;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class EntityB {
	@Id
	@GeneratedValue
	Long id;

	@OneToOne
	EntityC c;
}
