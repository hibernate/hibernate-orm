package org.hibernate.processor.test.staticquery;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Book {
	@Id
	String isbn;
	String title;
	boolean obsolete;
}
