package org.hibernate.processor.test.classnamecollision.somewhere;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
abstract public class Something {
	String name;
}
