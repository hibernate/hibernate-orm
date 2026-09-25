package org.hibernate.processor.test.wildcard;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Book {
	@Id
	String isbn;
	String title;
	// e.g. "crime", "romance", "fantasy"
	String genre;
}
