package org.hibernate.processor.test.data.restriction;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class DataRestrictionPublisher {
	@Id
	Long id;
	String name;
}
