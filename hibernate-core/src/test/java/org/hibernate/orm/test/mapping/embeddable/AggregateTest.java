/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;

import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;



@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonAggregate.class)
public class AggregateTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				JsonHolder.class, XmlHolder.class
		};
	}

	@BeforeEach
	public void setUp() {
		inTransaction(
				session -> {
					session.persist( new JsonHolder( 1L, Aggregate.createAggregate2() ) );
					session.persist( new XmlHolder( 1L, Aggregate.createAggregate2() ) );
				}
		);
	}

	@AfterEach
	protected void cleanupTest() {
		sessionFactoryScope().getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey("HHH-17294")
	public void testDirtyCheckingJsonAggregate() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					JsonHolder aggregateHolder = entityManager.find( JsonHolder.class, 1L );
					Assertions.assertEquals("String 'abc'", aggregateHolder.getAggregate().getTheString());
					aggregateHolder.getAggregate().setTheString( "MyString" );
					entityManager.flush();
					entityManager.clear();
					Assertions.assertEquals( "MyString", entityManager.find( JsonHolder.class, 1L ).getAggregate().getTheString() );
				}
		);
	}

	@Test
	@JiraKey("HHH-17294")
	public void testDirtyCheckingXmlAggregate() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					XmlHolder aggregateHolder = entityManager.find( XmlHolder.class, 1L );
					Assertions.assertEquals("String 'abc'", aggregateHolder.getAggregate().getTheString());
					aggregateHolder.getAggregate().setTheString( "MyString" );
					entityManager.flush();
					entityManager.clear();
					Assertions.assertEquals( "MyString", entityManager.find( XmlHolder.class, 1L ).getAggregate().getTheString() );
				}
		);
	}

	@Entity(name = "JsonHolder")
	public static class JsonHolder {

		@Id
		private Long id;
		@JdbcTypeCode(SqlTypes.JSON)
		private Aggregate aggregate;

		public JsonHolder() {
		}

		public JsonHolder(Long id, Aggregate aggregate) {
			this.id = id;
			this.aggregate = aggregate;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Aggregate getAggregate() {
			return aggregate;
		}

		public void setAggregate(Aggregate aggregate) {
			this.aggregate = aggregate;
		}
	}

	@Entity(name = "XmlHolder")
	public static class XmlHolder {

		@Id
		private Long id;
		@JdbcTypeCode(SqlTypes.SQLXML)
		private Aggregate aggregate;

		public XmlHolder() {
		}

		public XmlHolder(Long id, Aggregate aggregate) {
			this.id = id;
			this.aggregate = aggregate;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Aggregate getAggregate() {
			return aggregate;
		}

		public void setAggregate(Aggregate aggregate) {
			this.aggregate = aggregate;
		}
	}
}
