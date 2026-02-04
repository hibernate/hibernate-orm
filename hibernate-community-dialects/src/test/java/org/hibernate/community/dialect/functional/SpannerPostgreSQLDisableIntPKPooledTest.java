/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.functional;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.community.dialect.SpannerPostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;


@RequiresDialect(SpannerPostgreSQLDialect.class)
@DomainModel(annotatedClasses = {
		SpannerPostgreSQLDisableIntPKPooledTest.IntegerIdEntity.class,
		SpannerPostgreSQLDisableIntPKPooledTest.LongIdEntity.class
})
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.dialect.spannerpg.use_integer_for_primary_key", value = "false")
})
public class SpannerPostgreSQLDisableIntPKPooledTest {

	@Test
	@FailureExpected(reason = "Throws an error since generated Long value converted to int causing NonUniqueObjectException")
	public void testIntegerPooledSequences(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			for (int i = 0; i < 55; i++) {
				IntegerIdEntity intEntity = new IntegerIdEntity();
				session.persist(intEntity);
			}
		});
	}

	@Test
	@FailureExpected
	public void testLongPooledSequences(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			for (int i = 0; i < 55; i++) {
				LongIdEntity longEntity = new LongIdEntity();
				session.persist(longEntity);
			}
		});
	}

	@Entity(name = "IntegerIdEntity")
	public static class IntegerIdEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "int_seq")
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "LongIdEntity")
	public static class LongIdEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "long_seq")
		@SequenceGenerator()
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
