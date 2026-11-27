package org.hibernate.processor.test.data.constructormismatch;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Book {
	@Id
	Long id;
}
