package org.hibernate.processor.test.defaultsession;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Book {
	@Id String isbn;
	String title;
}
