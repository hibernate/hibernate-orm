/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.ast.internal.MultiKeyLoadHelper;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class BatchFetchStrategyLoadingTests {
	@Test
	@DomainModel( annotatedClasses = {
			BatchFetchStrategyLoadingTests.Thing1.class,
			BatchFetchStrategyLoadingTests.Thing2.class
	})
	@SessionFactory( useCollectingStatementInspector = true )
	public void testIt(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final Thing2 thing21 = session.getReference( Thing2.class, 1 );
			final Thing2 thing22 = session.getReference( Thing2.class, 2 );
			final Thing2 thing23 = session.getReference( Thing2.class, 3 );

			assertThat( statementInspector.getSqlQueries() ).isEmpty();
			assertThat( Hibernate.isInitialized( thing21 ) ).isFalse();
			assertThat( Hibernate.isInitialized( thing22 ) ).isFalse();
			assertThat( Hibernate.isInitialized( thing23 ) ).isFalse();

			final String name = thing21.getName();
			assertThat( name ).isEqualTo( "thing-2.1" );

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			if ( MultiKeyLoadHelper.supportsSqlArrayType( scope.getSessionFactory().getJdbcServices().getDialect() ) ) {
				assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 0 ), "?" ) ).isEqualTo( 1 );
			}
			else {
				assertThat( StringHelper.count( statementInspector.getSqlQueries().get( 0 ), "?" ) ).isEqualTo( 64 );
			}
			assertThat( Hibernate.isInitialized( thing21 ) ).isTrue();
			assertThat( Hibernate.isInitialized( thing22 ) ).isTrue();
			assertThat( Hibernate.isInitialized( thing23 ) ).isTrue();
		} );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Thing2 thing2 = new Thing2( 1, "thing-2.1" );
			final Thing1 thing1 = new Thing1( 1, "thing-1", thing2 );

			session.persist( thing2 );
			session.persist( thing1 );

			session.persist( new Thing2( 2, "thing-2.2" ) );
			session.persist( new Thing2( 3, "thing-2.3" ) );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "Thing1" )
	@Table( name = "Thing1" )
	public static class Thing1 {
		@Id
		private Integer id;
		@Basic
		private String name;
		@ManyToOne
		@JoinColumn( name = "thing2_fk" )
		private Thing2 thing2;

		protected Thing1() {
			// for use by Hibernate
		}

		public Thing1(Integer id, String name, Thing2 thing2) {
			this.id = id;
			this.name = name;
			this.thing2 = thing2;
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

		public Thing2 getThing2() {
			return thing2;
		}

		public void setThing2(Thing2 thing2) {
			this.thing2 = thing2;
		}
	}

	@Entity( name = "Thing2" )
	@Table( name = "Thing2" )
	@BatchSize( size = 64 )
	public static class Thing2 {
		@Id
		private Integer id;
		@Basic
		private String name;

		protected Thing2() {
			// for use by Hibernate
		}

		public Thing2(Integer id, String name) {
			this.id = id;
			this.name = name;
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
	}
}
