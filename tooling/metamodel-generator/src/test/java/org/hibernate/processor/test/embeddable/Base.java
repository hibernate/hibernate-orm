package org.hibernate.processor.test.embeddable;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class Base {

	@Id
	protected String uuid;
}
