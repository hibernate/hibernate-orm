/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.onetoone.bidirectional;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.engine.internal.StatisticalLoggingSessionEventListener;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
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
@SessionFactory
@DomainModel(annotatedClasses = {
		BidirectionalOneToOneInstanceTest.FooEntity.class,
		BidirectionalOneToOneInstanceTest.BarEntity.class
})
public class BidirectionalOneToOneInstanceTest {

	@Test
	public void testBidirectionalFetch(SessionFactoryScope scope) {
		String name = "foo_name";
		Date date = new Date();

		scope.inTransaction( session -> {
			BarEntity bar = new BarEntity();
			bar.setBusinessId( 1L );
			bar.setDate( date );

			FooEntity foo = new FooEntity();
			foo.setBusinessId( 2L );
			foo.setName( name );

			foo.setBar( bar );
			bar.setFoo( foo );

			session.persist( bar );
			session.persist( foo );
		} );

		scope.inTransaction( session -> {
			FooEntity foo = session.find( FooEntity.class, 1L );

			final AtomicInteger queryExecutionCount = new AtomicInteger();
			session.getEventListenerManager().addListener( new StatisticalLoggingSessionEventListener() {
				@Override
				public void jdbcExecuteStatementStart() {
					super.jdbcExecuteStatementStart();
					queryExecutionCount.getAndIncrement();
				}
			} );

			assertEquals( name, foo.getName() );

			BarEntity bar = foo.getBar();
			// no queries should be executed
			assertEquals( 0, queryExecutionCount.get() );
			assertEquals( date, bar.getDate() );

			FooEntity associatedFoo = bar.getFoo();
			// no queries should be executed
			assertEquals( 0, queryExecutionCount.get() );
			assertEquals( foo, associatedFoo );
		} );
	}

	@Entity(name = "FooEntity")
	@Table(name = "foo")
	public static class FooEntity {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "uuid", unique = true, updatable = false)
		private Long businessId;

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "bar_uuid", referencedColumnName = "uuid", nullable = false, updatable = false)
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
	@Table(name = "bar")
	public static class BarEntity {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "uuid", unique = true, updatable = false)
		private Long businessId;

		@OneToOne(fetch = FetchType.LAZY, mappedBy = "bar")
		private FooEntity foo;

		private Date date;

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

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}
	}
}
