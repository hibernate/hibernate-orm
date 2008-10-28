package org.hibernate.annotations.common.test.reflection.java.generics.deep;

import org.hibernate.annotations.common.test.reflection.java.generics.deep.Dummy;

public class GenericSuperclass1<T extends Dummy> {
	protected Long id;

	protected T dummy;


	public Long getId() {
		return id;
	}

	public T getDummy() {
		return dummy;
	}
}
