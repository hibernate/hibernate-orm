package org.hibernate.processor.test.data.async;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class AsyncBook {
	@Id
	public String isbn;

	public String title;
}
