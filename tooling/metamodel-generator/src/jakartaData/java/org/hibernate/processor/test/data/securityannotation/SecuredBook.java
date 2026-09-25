package org.hibernate.processor.test.data.securityannotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class SecuredBook {
	@Id
	String isbn;

	String title;

	protected SecuredBook() {
	}
}
