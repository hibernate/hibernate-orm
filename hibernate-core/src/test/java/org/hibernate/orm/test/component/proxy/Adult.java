package org.hibernate.orm.test.component.proxy;

import jakarta.persistence.Entity;

@Entity
public class Adult extends Person {

	public Adult() {
		someInitMethod();
	}

	@Override
	public void someInitMethod() {
	}
}
