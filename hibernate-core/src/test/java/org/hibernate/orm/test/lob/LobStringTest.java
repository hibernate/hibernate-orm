/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;

import java.io.UnsupportedEncodingException;
import java.sql.Clob;
import java.sql.SQLException;
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
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11477")
// Note that Cockroach doesn't support LOB functions. See https://github.com/cockroachdb/cockroach/issues/26725
@RequiresDialect(PostgreSQLDialect.class)
@DomainModel(
		annotatedClasses = LobStringTest.TestEntity.class
)
@SessionFactory
public class LobStringTest {

	private static final int LONG_STRING_SIZE = 3999;

	private final String value1 = buildRecursively( LONG_STRING_SIZE, 'x' );
	private final String value2 = buildRecursively( LONG_STRING_SIZE, 'y' );

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) throws Exception {
		TestEntity entity = new TestEntity();
		scope.inTransaction( session -> {

			entity.setFirstLobField( value1 );
			entity.setSecondLobField( value2 );
			entity.setClobField( session.getLobHelper().createClob( value2 ) );
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final TestEntity testEntity = session.find( TestEntity.class, entity.getId() );
			assertThat( testEntity.getFirstLobField(), is( value1 ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "delete from TestEntity" ).executeUpdate()
		);
	}

	@Test
	@JiraKey(value = "HHH-11477")
	public void testHqlQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Query query = session.createQuery( "from TestEntity" );

			final List<TestEntity> results = query.list();

			assertThat( results.size(), is( 1 ) );

			final TestEntity testEntity = results.get( 0 );
			assertThat( testEntity.getFirstLobField(), is( value1 ) );
			assertThat( testEntity.getSecondLobField(), is( value2 ) );
			final Clob clobField = testEntity.getClobField();
			try {

				assertThat( clobField.getSubString( 1, (int) clobField.length() ), is( value2 ) );
			}
			catch (SQLException e) {
				fail( e.getMessage() );
			}
		} );
	}

	@Test
	@JiraKey(value = "HHH-11477")
	@RequiresDialect(PostgreSQLDialect.class)
	public void testUsingStringLobAnnotatedPropertyInNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<TestEntity> results = session.createNativeQuery(
							"select te.* " +
									"from test_entity te " +
									"where lower(convert_from(lo_get(cast(te.firstLobField as oid)), 'UTF8')) LIKE :value",
							TestEntity.class
					)
					.setParameter( "value", value1 )
					.getResultList();

			assertThat( results.size(), is( 1 ) );

			final TestEntity testEntity = results.get( 0 );
			assertThat( testEntity.getFirstLobField(), is( value1 ) );
			assertThat( testEntity.getSecondLobField(), is( value2 ) );
			final Clob clobField = testEntity.getClobField();
			try {
				assertThat( clobField.getSubString( 1, (int) clobField.length() ), is( value2 ) );
			}
			catch (SQLException e) {
				fail( e.getMessage() );
			}
		} );
	}

	@Test
	@JiraKey(value = "HHH-11477")
	@RequiresDialect(PostgreSQLDialect.class)
	public void testSelectStringLobAnnotatedInNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<String> results = session.createNativeQuery(
							"select convert_from(lo_get(cast(te.secondLobField as oid)), 'UTF8') " +
									"from test_entity te " +
									"where lower(convert_from(lo_get(cast(te.firstLobField as oid)), 'UTF8')) LIKE :value" )
					.setParameter( "value", value1 )
					.list();

			assertThat( results.size(), is( 1 ) );

			assertThat( results.get( 0 ), is( value2 ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11477")
	@RequiresDialect(PostgreSQLDialect.class)
	public void testUsingLobPropertyInNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<String> results = session.createNativeQuery(
							"select convert_from(lo_get(cast(te.secondLobField as oid)), 'UTF8') " +
									"from test_entity te " +
									"where lower(convert_from(lo_get(cast(te.clobField as oid)), 'UTF8')) LIKE :value" )
					.setParameter( "value", value2 )
					.list();

			assertThat( results.size(), is( 1 ) );

			assertThat( results.get( 0 ), is( value2 ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11477")
	@RequiresDialect(PostgreSQLDialect.class)
	public void testSelectClobPropertyInNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<byte[]> results = session.createNativeQuery(
							"select lo_get(cast(te.clobField as oid)) " +
									"from test_entity te " +
									"where lower(convert_from(lo_get(cast(te.clobField as oid)), 'UTF8')) LIKE :value" )
					.setParameter( "value", value2 )
					.list();

			assertThat( results.size(), is( 1 ) );

			try {
				assertThat( new String( results.get( 0 ), "UTF8" ), is( value2 ) );
			}
			catch (UnsupportedEncodingException e) {
				fail( e.getMessage() );
			}
		} );
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		@Id
		@GeneratedValue
		private long id;

		@Lob
		@Column(length = LONG_STRING_SIZE) //needed by HSQLDialect
		String firstLobField;

		@Lob
		@Column(length = LONG_STRING_SIZE) //needed by HSQLDialect
		String secondLobField;

		@Lob
		@Column(length = LONG_STRING_SIZE) //needed by HSQLDialect
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


	private String buildRecursively(int size, char baseChar) {
		StringBuilder buff = new StringBuilder();
		for ( int i = 0; i < size; i++ ) {
			buff.append( baseChar );
		}
		return buff.toString();
	}
}
