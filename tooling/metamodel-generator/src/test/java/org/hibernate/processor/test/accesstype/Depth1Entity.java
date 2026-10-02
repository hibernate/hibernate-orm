package org.hibernate.processor.test.accesstype;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Depth1Entity {

	@Id
	Long id;

	String probe;
}
