/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.batch;

import static org.junit.Assert.assertNotNull;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Cacheable;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;

/**
 * @author Guillaume Toison
 */
@DomainModel( annotatedClasses = {
		BatchEntityRowOrderWithSelectFetchWithDisabledProxyTest.Container.class,
		BatchEntityRowOrderWithSelectFetchWithDisabledProxyTest.ValueEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17983" )
public class BatchEntityRowOrderWithSelectFetchWithDisabledProxyTest {

	@Test
	public void testHQLQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.getSessionFactory().getCache().evict( ValueEntity.class );
			
			final Container result = session.createQuery(
					"from Container",
					Container.class
			).getSingleResult();
			
			assertNotNull( result );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// The order of the values is carefully chosen so at some point we batch load value1 and value2
			// Since value1's PK < value2's PK we will initialize value1 before value2, even if we were trying to load value2
			final ValueEntity value1 = new ValueEntity();
			final ValueEntity value2 = new ValueEntity();
			final ValueEntity value3 = new ValueEntity();
			final ValueEntity value4 = new ValueEntity();
			value1.next = value2;
			value2.next = value1;
			value3.next = value2;
			value4.next = value2;
			session.persist( value1 );
			session.persist( value2 );
			session.persist( value3 );
			session.persist( value4 );
			
			final Container container1 = new Container();
			container1.value1 = value4;
			container1.value2 = value3;
			container1.value3 = value1;
			session.persist( container1 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Container" ).executeUpdate();
			session.createMutationQuery( "delete from ValueEntity" ).executeUpdate();
		} );
	}

	@Proxy(lazy = false)
	@Entity( name = "Container" )
	static class Container {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		
		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(FetchMode.SELECT)
		private ValueEntity value1;
		
		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(FetchMode.SELECT)
		private ValueEntity value2;
		
		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(FetchMode.SELECT)
		private ValueEntity value3;
	}

	@BatchSize( size = 2 )
	@Proxy(lazy = false)
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "type")
	@DiscriminatorValue(value = "VALUE")
	@Cacheable(value = true)
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Entity( name = "ValueEntity" )
	static class ValueEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Nonnull
		@ManyToOne(fetch = FetchType.LAZY)
		private ValueEntity next;
	}
}
