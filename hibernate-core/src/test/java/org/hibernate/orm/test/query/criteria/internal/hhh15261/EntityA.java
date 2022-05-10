package org.hibernate.orm.test.query.criteria.internal.hhh15261;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class EntityA {
	@Id
	@GeneratedValue
	Long id;

	@ManyToOne
	private EntityB b;
}
