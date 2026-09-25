package org.hibernate.processor.annotation;

class OrderBy {
	String fieldName;
	boolean descending;
	boolean ignoreCase;

	public OrderBy(String fieldName, boolean descending, boolean ignoreCase) {
		this.fieldName = fieldName;
		this.descending = descending;
		this.ignoreCase = ignoreCase;
	}
}
