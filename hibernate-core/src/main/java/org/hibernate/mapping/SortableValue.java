package org.hibernate.mapping;

public interface SortableValue {

	boolean isSorted();

	int[] sortProperties();
}
