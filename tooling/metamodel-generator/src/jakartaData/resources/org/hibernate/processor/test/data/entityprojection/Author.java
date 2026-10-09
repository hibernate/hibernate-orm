package org.hibernate.processor.test.data.entityprojection;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Author {
	@Id
	String ssn;
}
