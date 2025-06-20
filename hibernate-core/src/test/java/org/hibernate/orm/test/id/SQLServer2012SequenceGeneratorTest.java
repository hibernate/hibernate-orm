/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/id/SQLServer2012Person.hbm.xml"
)
@SessionFactory
public class SQLServer2012SequenceGeneratorTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	/**
	 * SQL server requires that sequence be initialized to something other than the minimum value for the type
	 * (e.g., Long.MIN_VALUE). For generator = "sequence", the initial value must be provided as a parameter.
	 * For this test, the sequence is initialized to 10.
	 */
	@Test
	@JiraKey(value = "HHH-8814")
	@RequiresDialect(value = SQLServerDialect.class, majorVersion = 11)
	public void testStartOfSequence(SessionFactoryScope scope) {
		final Person person = scope.fromTransaction( session -> {
			final Person _person = new Person();
			session.persist( _person );
			return _person;
		} );

		assertEquals( 10, person.getId() );
	}
}
