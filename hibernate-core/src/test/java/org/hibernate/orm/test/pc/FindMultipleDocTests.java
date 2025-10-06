/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.hibernate.OrderedReturn.ORDERED;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = FindMultipleDocTests.Person.class)
@SessionFactory
public class FindMultipleDocTests {
	@Test
	void testUsage(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			//tag::pc-find-multiple-example[]
			List<Person> persons = session.findMultiple(
					Person.class,
					List.of(1,2,3),
					PESSIMISTIC_WRITE,
					ORDERED
			);
			//end::pc-find-multiple-example[]
		} );
	}

	@Entity(name="Person")
	@Table(name="persons")
	public static class Person {
		@Id
		private Integer id;
		private String name;
	}
}
