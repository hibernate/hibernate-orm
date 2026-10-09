package org.hibernate.processor.test.data.entityprojection;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

@Entity
public class Book {
	@Id
	String isbn;

	@ManyToMany
	Set<Author> authors;
}
