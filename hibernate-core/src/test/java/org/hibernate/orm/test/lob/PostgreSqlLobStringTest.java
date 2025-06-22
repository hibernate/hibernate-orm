/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;

import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11614")
@RequiresDialect(PostgreSQLDialect.class)
@DomainModel(
		annotatedClasses = PostgreSqlLobStringTest.TestEntity.class
)
@SessionFactory
public class PostgreSqlLobStringTest {

	private final String value1 = "abc";
	private final String value2 = "def";
	private final String value3 = "ghi";

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope)
			throws Exception {

		scope.inTransaction(
				session ->
						session.doWork( connection -> {
							try (PreparedStatement statement = connection.prepareStatement(
									"insert \n" +
											"    into\n" +
											"        TEST_ENTITY\n" +
											"        (firstLobField, secondLobField, clobfield, id) \n" +
											"    values\n" +
											"        (?, ?, ?, -1)"
							)) {
								int index = 1;
								statement.setClob( index++, session.getLobHelper().createClob( value1 ) );
								statement.setClob( index++, session.getLobHelper().createClob( value2 ) );
								statement.setClob( index++, session.getLobHelper().createClob( value3 ) );

								assertEquals( 1, statement.executeUpdate() );
							}
						} )
		);
	}

	@Test
	public void testBadClobDataSavedAsStringFails(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query query = session.createQuery( "from TestEntity" );

					final List<TestEntity> results = query.list();

					assertThat( results.size(), is( 1 ) );

					final TestEntity testEntity = results.get( 0 );
					assertThat( testEntity.getFirstLobField(), is( value1 ) );
					assertThat( testEntity.getSecondLobField(), is( value2 ) );
					final Clob clobField = testEntity.getClobField();
					try {

						assertThat( clobField.getSubString( 1, (int) clobField.length() ), is( value3 ) );
					}
					catch (SQLException e) {
						fail( e.getMessage() );
					}
				}
		);
	}

	@Test
	public void testBadClobDataSavedAsStringworksAfterUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					session.doWork( connection -> {
						try (Statement statement = connection.createStatement()) {
							statement.executeUpdate(
									"update test_entity\n" +
											"set \n" +
											"    clobfield = lo_from_bytea(0, lo_get(clobfield)),\n" +
											"    firstlobfield = lo_from_bytea(0, lo_get(firstlobfield)),\n" +
											"    secondlobfield = lo_from_bytea(0, lo_get(secondlobfield))"
							);
						}
					} );
				} );

		scope.inTransaction(
				session -> {
					final Query query = session.createQuery( "from TestEntity" );

					final List<TestEntity> results = query.list();

					assertThat( results.size(), is( 1 ) );

					final TestEntity testEntity = results.get( 0 );
					assertThat( testEntity.getFirstLobField(), is( value1 ) );
					assertThat( testEntity.getSecondLobField(), is( value2 ) );
					final Clob clobField = testEntity.getClobField();
					try {

						assertThat( clobField.getSubString( 1, (int) clobField.length() ), is( value3 ) );
					}
					catch (SQLException e) {
						fail( e.getMessage() );
					}
				} );
	}

	@AfterEach
	public void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		@Id
		@GeneratedValue
		private long id;

		@Lob
		@Column
		String firstLobField;

		@Lob
		@Column
		String secondLobField;

		@Lob
		@Column
		Clob clobField;

		public long getId() {
			return id;
		}

		public String getFirstLobField() {
			return firstLobField;
		}

		public void setFirstLobField(String firstLobField) {
			this.firstLobField = firstLobField;
		}

		public String getSecondLobField() {
			return secondLobField;
		}

		public void setSecondLobField(String secondLobField) {
			this.secondLobField = secondLobField;
		}

		public Clob getClobField() {
			return clobField;
		}

		public void setClobField(Clob clobField) {
			this.clobField = clobField;
		}
	}

//	@Override
//	protected boolean isCleanupTestDataRequired() {
//		return true;
//	}

//	protected boolean isCleanupTestDataUsingBulkDelete() {
//		return true;
//	}
}
