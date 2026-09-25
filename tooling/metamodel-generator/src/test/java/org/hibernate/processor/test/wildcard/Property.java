package org.hibernate.processor.test.wildcard;

public interface Property<T> {

	String getName();

	T getValue();
}
