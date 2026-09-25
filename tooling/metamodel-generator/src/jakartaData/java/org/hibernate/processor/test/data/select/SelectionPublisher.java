package org.hibernate.processor.test.data.select;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class SelectionPublisher {
	@Id
	Long id;

	String name;
}
