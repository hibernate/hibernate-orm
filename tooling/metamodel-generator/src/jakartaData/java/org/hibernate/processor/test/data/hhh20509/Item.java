package org.hibernate.processor.test.data.hhh20509;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Item {
	@Id
	@GeneratedValue
	Long id;

	String name;

	protected Item() {
	}

	public Item(String name) {
		this.name = name;
	}
}
