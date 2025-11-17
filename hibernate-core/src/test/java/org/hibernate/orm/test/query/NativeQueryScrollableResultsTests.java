/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey( "HHH-11033" )
@DomainModel( annotatedClasses = NativeQueryScrollableResultsTests.MyEntity.class )
@SessionFactory
public class NativeQueryScrollableResultsTests {

	@Test
	public void testSetParameters(SessionFactoryScope scope) {
		scope.inTransaction( (s) -> {
			final List<BigInteger> params = new ArrayList<>();
			params.add( new BigInteger( "2" ) );
			params.add( new BigInteger( "3" ) );

			final Query<BigInteger> query = s.createNativeQuery( "select e.big from MY_ENTITY e where e.big in (:bigValues)", BigInteger.class )
					.setParameter( "bigValues", params );
			try (ScrollableResults<BigInteger> scroll = query.scroll()) {
				while ( scroll.next() ) {
					assertThat( scroll.get() ).isNotNull();
					assertThat( scroll.get() ).isNotNull();
				}
			}

		} );
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( (session) -> {
			session.persist( new MyEntity( 1L, "entity_1", new BigInteger( "3" ) ) );
			session.persist( new MyEntity( 2L, "entity_2", new BigInteger( "6" ) ) );
		} );
	}

	@AfterEach
	protected void dropTestData(SessionFactoryScope scope) throws Exception {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	public static class MyEntity {
		@Id
		private Long id;

		private BigInteger big;

		private String description;

		public MyEntity() {
		}

		public MyEntity(Long id, String description, BigInteger big) {
			this.id = id;
			this.description = description;
			this.big = big;
		}

		public String getDescription() {
			return description;
		}
	}
}
