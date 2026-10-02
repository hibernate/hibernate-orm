package org.hibernate.processor.test.data.innerclass;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class HandwrittenEntity {
	@Id
	@GeneratedValue
	public Long id;

	public String name;
}
