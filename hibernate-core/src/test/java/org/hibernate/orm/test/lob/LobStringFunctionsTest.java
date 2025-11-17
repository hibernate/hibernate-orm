/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;

import java.sql.Clob;
import java.util.List;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.core.Is.is;
import static org.hibernate.Hibernate.getLobHelper;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-15162")
@DomainModel(annotatedClasses = LobStringFunctionsTest.TestEntity.class)
@SessionFactory
@SkipForDialect(dialectClass = InformixDialect.class, reason = "Informix does not allow these functions for LOBs")
public class LobStringFunctionsTest {

	private static final int LONG_STRING_SIZE = 3999;

	private final String value1 = buildRecursively( LONG_STRING_SIZE, 'x' );
	private final String value2 = buildRecursively( LONG_STRING_SIZE, 'y' );

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) throws Exception {
		TestEntity entity = new TestEntity();
		scope.inTransaction( session -> {

			entity.setFirstLobField( value1 );
			entity.setSecondLobField( value2 );
			entity.setClobField( getLobHelper().createClob( value2 ) );
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final TestEntity testEntity = session.find( TestEntity.class, entity.getId() );
			assertThat( testEntity.getFirstLobField(), is( value1 ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testLengthFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Query<Tuple> query = session.createQuery(
					"select length(e.firstLobField),length(e.secondLobField),length(e.clobField) from TestEntity e",
					Tuple.class
			);

			final List<Tuple> results = query.getResultList();

			assertThat( results.size(), is( 1 ) );

			final Tuple testEntity = results.get( 0 );
			assertThat( testEntity.get( 0, Integer.class ), is( value1.length() ) );
			assertThat( testEntity.get( 1, Integer.class ), is( value2.length() ) );
			assertThat( testEntity.get( 2, Integer.class ), is( value2.length() ) );
		} );
	}

	@Test
	public void testOctetLengthFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Query<Tuple> query = session.createQuery(
					"select octet_length(e.firstLobField),octet_length(e.secondLobField),octet_length(e.clobField) from TestEntity e",
					Tuple.class
			);

			final List<Tuple> results = query.getResultList();

			assertThat( results.size(), is( 1 ) );

			final Tuple testEntity = results.get( 0 );
			// Some databases report 2 octets per character
			assertThat( testEntity.get( 0, Integer.class ), isOneOf( value1.length(), value1.length() * 2 ) );
			assertThat( testEntity.get( 1, Integer.class ), isOneOf( value2.length(), value2.length() * 2 ) );
			assertThat( testEntity.get( 2, Integer.class ), isOneOf( value2.length(), value2.length() * 2 ) );
		} );
	}

	@Test
	public void testBitLengthFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Query<Tuple> query = session.createQuery(
					"select bit_length(e.firstLobField),bit_length(e.secondLobField),bit_length(e.clobField) from TestEntity e",
					Tuple.class
			);

			final List<Tuple> results = query.getResultList();

			assertThat( results.size(), is( 1 ) );

			final Tuple testEntity = results.get( 0 );
			// Some databases report 16 bit per character
			assertThat( testEntity.get( 0, Integer.class ), isOneOf( value1.length() * 8, value1.length() * 16 ) );
			assertThat( testEntity.get( 1, Integer.class ), isOneOf( value2.length() * 8, value2.length() * 16 ) );
			assertThat( testEntity.get( 2, Integer.class ), isOneOf( value2.length() * 8, value2.length() * 16 ) );
		} );
	}

	@Test
	public void testConcatFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// Use trim('') instead of '' since Sybase interprets that as single space string...
			final Query<Tuple> query = session.createQuery(
					"select concat(e.firstLobField, trim('')),concat(e.secondLobField, trim('')),concat(e.clobField, trim('')) from TestEntity e",
					Tuple.class
			);

			final List<Tuple> results = query.getResultList();

			assertThat( results.size(), is( 1 ) );

			final Tuple testEntity = results.get( 0 );
			assertThat( testEntity.get( 0, String.class ), is( value1 ) );
			assertThat( testEntity.get( 1, String.class ), is( value2 ) );
			assertThat( testEntity.get( 2, String.class ), is( value2 ) );
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
