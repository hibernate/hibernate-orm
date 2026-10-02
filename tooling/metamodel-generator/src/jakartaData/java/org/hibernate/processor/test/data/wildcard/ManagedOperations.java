package org.hibernate.processor.test.data.wildcard;

public interface ManagedOperations<E> {
	@SuppressWarnings("unchecked")
	default Class<? extends E> getEntityClass() {
		return (Class<? extends E>) Object.class;
	}
}
