/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.bidirectional;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@SessionFactory(useCollectingStatementInspector = true)
@DomainModel(annotatedClasses = {
		BidirectionalOneToOneLazyFKTest.FooEntity.class,
		BidirectionalOneToOneLazyFKTest.BarEntity.class
})
public class BidirectionalOneToOneLazyFKTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			BarEntity bar = new BarEntity();
			bar.setBusinessId( 1L );
			bar.setaDouble( 0.5 );

			FooEntity foo = new FooEntity();
			foo.setBusinessId( 2L );
			foo.setName( "foo_name" );

			foo.setBar( bar );
			bar.setFoo( foo );

			session.persist( bar );
			session.persist( foo );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from FooEntity" ).executeUpdate();
			session.createMutationQuery( "delete from BarEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testBidirectionalFetchJoinColumnSide(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			FooEntity foo = session.find( FooEntity.class, 1L );
			statementInspector.clear();

			BarEntity bar = foo.getBar();
			statementInspector.assertExecutedCount( 0 );
			assertEquals( 0.5, bar.getaDouble() );

			FooEntity associatedFoo = bar.getFoo();
			statementInspector.assertExecutedCount( 0 );
			assertEquals( "foo_name", associatedFoo.getName() );
			assertEquals( foo, associatedFoo );

			assertEquals( bar, associatedFoo.getBar() );
		} );
	}

	@Test
	public void testBidirectionalFetchMappedBySide(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			BarEntity bar = session.find( BarEntity.class, 1L );
			statementInspector.clear();

			FooEntity foo = bar.getFoo();
			statementInspector.assertExecutedCount( 0 );
			assertEquals( "foo_name", foo.getName() );

			BarEntity associatedBar = foo.getBar();
			statementInspector.assertExecutedCount( 0 );
			assertEquals( 0.5, associatedBar.getaDouble() );
			assertEquals( bar, associatedBar );

			assertEquals( foo, associatedBar.getFoo() );
		} );
	}

	@Entity(name = "FooEntity")
	@Table(name = "foo_table")
	public static class FooEntity {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "business_id", unique = true, updatable = false)
		private Long businessId;

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "bar_business_id", referencedColumnName = "business_id", nullable = false, updatable = false)
		private BarEntity bar;

		private String name;

		public BarEntity getBar() {
			return bar;
		}

		public void setBar(BarEntity bar) {
			this.bar = bar;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getBusinessId() {
			return businessId;
		}

		public void setBusinessId(Long businessId) {
			this.businessId = businessId;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "BarEntity")
	@Table(name = "bar_table")
	public static class BarEntity {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "business_id", unique = true, updatable = false)
		private Long businessId;

		@OneToOne(fetch = FetchType.LAZY, mappedBy = "bar")
		private FooEntity foo;

		private Double aDouble;

		public FooEntity getFoo() {
			return foo;
		}

		public void setFoo(FooEntity foo) {
			this.foo = foo;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getBusinessId() {
			return businessId;
		}

		public void setBusinessId(Long businessId) {
			this.businessId = businessId;
		}

		public Double getaDouble() {
			return aDouble;
		}

		public void setaDouble(Double aDouble) {
			this.aDouble = aDouble;
		}
	}
}
