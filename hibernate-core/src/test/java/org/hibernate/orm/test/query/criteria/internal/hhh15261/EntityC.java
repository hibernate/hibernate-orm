package org.hibernate.orm.test.query.criteria.internal.hhh15261;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class EntityC {
	@Id
	@GeneratedValue
	Long id;
}

