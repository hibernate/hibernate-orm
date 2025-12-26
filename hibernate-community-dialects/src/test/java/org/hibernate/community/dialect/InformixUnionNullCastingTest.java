/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Lob;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@RequiresDialect(org.hibernate.community.dialect.InformixDialect.class)
@DomainModel(annotatedClasses = {
		InformixUnionNullCastingTest.BaseEntity.class,
		InformixUnionNullCastingTest.LvarcharEntity.class,
		InformixUnionNullCastingTest.DatetimeEntity.class,
		InformixUnionNullCastingTest.ClobEntity.class,
		InformixUnionNullCastingTest.EmptyEntity.class
})
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(
		settings = {
				@Setting(
						name = AvailableSettings.DIALECT,
						value = "org.hibernate.community.dialect.InformixDialect"
				),
				@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "true"),
		}
)
public class InformixUnionNullCastingTest {

	@BeforeEach
	protected void setupTest(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.persist(new EmptyEntity());

					LvarcharEntity lvarcharEntity = new LvarcharEntity();
					lvarcharEntity.longContent = "This is a very long string designed to be stored as LVARCHAR in Informix.";
					session.persist(lvarcharEntity);

					ClobEntity clobEntity = new ClobEntity();
					clobEntity.clobContent = "This is a CLOB content that should be cast correctly in union queries.";
					session.persist(clobEntity);

					DatetimeEntity datetimeEntity = new DatetimeEntity();
					datetimeEntity.eventTime = LocalDateTime.now();
					session.persist(datetimeEntity);
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-19974")
	public void testInformixSpecialTypeCasting(SessionFactoryScope scope) {
		scope.inTransaction(session ->
				session.createQuery(
						"select r from root_entity r",
						BaseEntity.class
				).list()
		);

		SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		List<String> sqlQueries = inspector.getSqlQueries();

		sqlQueries.forEach(System.out::println);

		Stream.of(
				new CastExpectation("LVARCHAR", "cast(null as lvarchar)"),
				new CastExpectation("DATETIME", "cast(null as datetime", "year to"),
				new CastExpectation("CLOB", "cast(null as clob)")
		).forEach(expectation -> verifyCastings(sqlQueries, expectation));
	}


	private void verifyCastings(List<String> sqlQueries, CastExpectation expectation) {
		assertThat(sqlQueries)
				.as("SQL should contain proper cast for type: %s", expectation.typeName)
				.anyMatch(sql -> {
					String lowerSql = sql.toLowerCase();
					return expectation.requiredPatterns.stream()
							.allMatch(lowerSql::contains);
				});
	}

	/**
	 * A simple record to hold expectation data for verification.
	 *
	 * @param typeName The name of the type, used for error messaging.
	 * @param requiredPatterns The list of lowercase string patterns that must be present in the generated SQL.
	 */
	record CastExpectation(String typeName, List<String> requiredPatterns) {
		CastExpectation(String typeName, String... patterns) {
			this(typeName, List.of(patterns));
		}
	}


	@Entity(name = "root_entity")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static abstract class BaseEntity {
		@Id
		@GeneratedValue
		private Long id;
	}

	@Entity(name = "lvarchar_entity")
	public static class LvarcharEntity extends BaseEntity {

		@JdbcTypeCode(SqlTypes.LONGVARCHAR)
		public String longContent;
	}

	@Entity(name = "datetime_entity")
	public static class DatetimeEntity extends BaseEntity {

		public LocalDateTime eventTime;
	}

	@Entity(name = "clob_entity")
	public static class ClobEntity extends BaseEntity {

		@Lob
		@JdbcTypeCode(SqlTypes.CLOB)
		public String clobContent;
	}

	@Entity(name = "empty_entity")
	public static class EmptyEntity extends BaseEntity {
	}

}
