package org.hibernate.processor.test.data.generic;

import jakarta.persistence.Entity;

@Entity
public class MyActualEntity extends MyMappedSuperclass<Integer> {
	public String myString;
}
