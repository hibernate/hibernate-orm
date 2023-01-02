/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.engine.internal.StatisticalLoggingSessionEventListener;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
		BidirectionalOneToOneWithConverterTest.FooEntity.class,
		BidirectionalOneToOneWithConverterTest.BarEntity.class,
})
@JiraKey("HHH-15950")
public class BidirectionalOneToOneWithConverterTest {
	@Test
	public void testBidirectionalFetch(SessionFactoryScope scope) {
		String name = "foo_name";
		Date date = new Date();

		scope.inTransaction( session -> {
			BarEntity bar = new BarEntity();
			bar.setBusinessId( new BusinessId( UUID.randomUUID().toString() ) );
			bar.setDate( date );

			FooEntity foo = new FooEntity();
			foo.setBusinessId( new BusinessId( UUID.randomUUID().toString() ) );
			foo.setName( name );

			foo.setBar( bar );
			bar.setFoo( foo );

			session.persist( bar );
			session.persist( foo );
		} );

		scope.inTransaction( session -> {
			FooEntity foo = session.find( FooEntity.class, 1L );
			assertEquals( name, foo.getName() );

			final AtomicInteger queryExecutionCount = new AtomicInteger();
			session.getEventListenerManager().addListener( new StatisticalLoggingSessionEventListener() {
				@Override
				public void jdbcExecuteStatementStart() {
					super.jdbcExecuteStatementStart();
					queryExecutionCount.getAndIncrement();
				}
			} );

			BarEntity bar = foo.getBar();
			// no queries should be executed
			assertEquals( 0, queryExecutionCount.get() );
			assertEquals( date, bar.getDate() );

			 FooEntity associatedFoo = bar.getFoo();
			// no queries should be executed
			 assertEquals(0, queryExecutionCount.get());
			 assertEquals( foo, associatedFoo );
		} );
	}

	// todo marco : verifica che get su associazione non faccia altra query
	//  foo.getBar() - non deve fare query
	//  bar.getFoo() - non deve fare query + deve essere stessa instance di quello col find
	// todo marco : provare anche contrario (session.find(Bar.class, 1L);

	// todo marco : fare un altro test con associazione EAGER
	//  questo dovrebbe fare il detect della circularity

	public static class BusinessId implements Serializable {
		private String value;

		public BusinessId() {
		}

		public BusinessId(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	public static class BusinessIdConverter implements AttributeConverter<BusinessId, String> {
		@Override
		public String convertToDatabaseColumn(BusinessId uuid) {
			return uuid != null ? uuid.getValue() : null;
		}

		@Override
		public BusinessId convertToEntityAttribute(String s) {
			return s == null ? null : new BusinessId( s );
		}
	}

	@Entity
	@Table(name = "foo")
	public static class FooEntity {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "uuid", unique = true, updatable = false)
		@Convert(converter = BusinessIdConverter.class)
		private BusinessId businessId;

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

		public BusinessId getBusinessId() {
			return businessId;
		}

		public void setBusinessId(BusinessId businessId) {
			this.businessId = businessId;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity
	@Table(name = "bar")
	public static class BarEntity {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "uuid", unique = true, updatable = false)
		@Convert(converter = BusinessIdConverter.class)
		private BusinessId businessId;

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

		public BusinessId getBusinessId() {
			return businessId;
		}

		public void setBusinessId(BusinessId businessId) {
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
