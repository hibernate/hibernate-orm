package org.hibernate.processor.test.hqlvalidation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
class Author {
	@Id
	String ssn;
}
