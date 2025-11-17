/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.write;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = OptionalTableUpdateTests.TheEntity.class)
@SessionFactory
public class OptionalTableUpdateTests {
	@Test
	void testUpsertInsert(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "name", null ) );
		} );

		verifySecondaryRows( scope, 0 );

		scope.inTransaction( (session) -> {
			final TheEntity loaded = session.byId( TheEntity.class ).load( 1 );
			loaded.setDetails( "non-null" );
		} );

		verifySecondaryRows( scope, 1 );
	}


	private static void verifySecondaryRows(String table, int expectedCount, SessionFactoryScope sfScope) {
		final String sql = "select count(1) from " + table;
		sfScope.inTransaction( (session) -> {
			final int count = (int) session.createNativeQuery( sql, Integer.class ).getSingleResult();
			assertThat( count ).isEqualTo( expectedCount );
		} );
	}

	private static void verifySecondaryRows(SessionFactoryScope scope, int expectedCount) {
		verifySecondaryRows( "supplements", expectedCount, scope );
	}

	@Test
	void testUpsertUpdate(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "name", "non-null" ) );
		} );

		verifySecondaryRows( scope, 1 );

		scope.inTransaction( (session) -> {
			final TheEntity loaded = session.byId( TheEntity.class ).load( 1 );
			loaded.setDetails( "non-non-null" );
		} );

		verifySecondaryRows( scope, 1 );
	}

	@Test
	void testUpsertDelete(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "name", "non-null" ) );
		} );

		verifySecondaryRows( scope, 1 );

		scope.inTransaction( (session) -> {
			final TheEntity loaded = session.byId( TheEntity.class ).load( 1 );
			loaded.setDetails( null );
		} );

		verifySecondaryRows( scope, 0 );
	}

	@AfterEach
	void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "TheEntity" )
	@Table( name = "entities" )
	@SecondaryTable( name = "supplements" )
	public static class TheEntity {
		@Id
		private Integer id;
		@Basic
		private String name;
		@Basic
		@Column( table = "supplements" )
		private String details;
		// Used to provoke https://hibernate.atlassian.net/browse/HHH-19749
		@Column( precision = 6, scale = 2, table = "supplements" )
		private BigDecimal theBigDecimal;
		// Used to provoke https://hibernate.atlassian.net/browse/HHH-18860
		@JdbcTypeCode( SqlTypes.NUMERIC )
		@Column( precision = 6, scale = 2, table = "supplements" )
		private Double theDoubleDecimal;

		private TheEntity() {
			// for use by Hibernate
		}

		public TheEntity(Integer id, String name, String details) {
			this.id = id;
			this.name = name;
			this.details = details;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDetails() {
			return details;
		}

		public void setDetails(String details) {
			this.details = details;
		}

		public BigDecimal getTheBigDecimal() {
			return theBigDecimal;
		}

		public void setTheBigDecimal(BigDecimal discount) {
			this.theBigDecimal = discount;
		}

		public Double getTheDoubleDecimal() {
			return theDoubleDecimal;
		}

		public void setTheDoubleDecimal(Double theDoubleDecimal) {
			this.theDoubleDecimal = theDoubleDecimal;
		}
	}
}
