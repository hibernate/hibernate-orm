/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.enhanced;

import static org.junit.Assert.assertEquals;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * {@inheritDoc}
 *
 * @author Yanming Zhou
 */
@JiraKey(value = "HHH-14219")
@DomainModel(
		annotatedClasses = {
				SharedSequenceTest.BaseEntity.class,
				SharedSequenceTest.Foo.class,
				SharedSequenceTest.Bar.class
		}
)
@SessionFactory
@RequiresDialect(MySQLDialect.class)
public class SharedSequenceTest {

	@Test
	public void testSequenceTableContainsOnlyOneRow(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Query<Number> q = session.createNativeQuery("select count(*) from " + BaseEntity.SHARED_SEQ_NAME);
			assertEquals(1, q.uniqueResult().intValue());
		} );
	}

	@MappedSuperclass
	public static class BaseEntity {

		public static final String SHARED_SEQ_NAME = "shared_seq";

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO, generator = SHARED_SEQ_NAME)
		protected Long id;

	}

	@Entity
	public static class Foo extends BaseEntity {

	}

	@Entity
	public static class Bar extends BaseEntity {

	}

}
