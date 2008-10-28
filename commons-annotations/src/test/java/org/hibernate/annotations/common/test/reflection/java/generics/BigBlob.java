package org.hibernate.annotations.common.test.reflection.java.generics;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BigBlob<T, E extends Collection> {

	public E simpleGenericType() {
		return null;
	}

	public Class<?> genericClass() {
		return null;
	}

	public Class<T> genericType() {
		return null;
	}

	public Map<T, ? extends E> genericCollection() {
		return null;
	}

	public E[] array() {
		return null;
	}

	public List<? extends T>[] complexGenericArray() {
		return null;
	}
}
