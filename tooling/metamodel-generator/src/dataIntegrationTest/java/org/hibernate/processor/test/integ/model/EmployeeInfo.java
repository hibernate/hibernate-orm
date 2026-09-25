package org.hibernate.processor.test.integ.model;

/**
 * A record containing a subset of entity attributes.
 * Record component names match the entity attribute names.
 */
public record EmployeeInfo(
		Long id,
		String name,
		String department) {
}
