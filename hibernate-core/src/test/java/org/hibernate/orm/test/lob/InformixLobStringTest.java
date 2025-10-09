/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.hamcrest.MatcherAssert;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Clob;
import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hibernate.Hibernate.getLobHelper;

/**
 * @author Andrea Boriero
 * @author VladoKuruc
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-8511")
@DomainModel(annotatedClasses = InformixLobStringTest.TestEntity.class)
@SessionFactory
public class InformixLobStringTest {
	private final String value1 = "xxxxxxxxxx".repeat( 20 );
	private final String value2 = "yyyyyyyyyy".repeat( 20 );

	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		var entity = new TestEntity();
		factoryScope.inTransaction( (session) -> {
			entity.setFirstLobField( value1 );
			entity.setSecondLobField( value2 );
			entity.setClobField( getLobHelper().createClob( value2 ) );
			session.persist( entity );
		} );
		factoryScope.inTransaction( (session) -> {
			var testEntity = session.find( TestEntity.class, entity.getId() );
			MatcherAssert.assertThat( testEntity.getFirstLobField(), is( value1 ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey("HHH-8511")
	public void testHqlQuery(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			//noinspection removal
			var query = session.createQuery( "from TestEntity", TestEntity.class );
			final List<TestEntity> results = query.list();
			MatcherAssert.assertThat( results.size(), is( 1 ) );

			final TestEntity testEntity = results.get( 0 );
			MatcherAssert.assertThat( testEntity.getFirstLobField(), is( value1 ) );
			MatcherAssert.assertThat( testEntity.getSecondLobField(), is( value2 ) );
			final Clob clobField = testEntity.getClobField();
			try {
				MatcherAssert.assertThat( clobField.getSubString( 1, (int) clobField.length() ), is( value2 ) );
			}
			catch (SQLException e) {
				Assertions.fail( e.getMessage() );
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
		String firstLobField;

		@Lob
		String secondLobField;

		@Lob
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

}
