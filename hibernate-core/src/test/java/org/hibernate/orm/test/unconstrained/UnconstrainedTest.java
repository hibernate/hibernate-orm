/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unconstrained;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.hibernate.cache.spi.CacheImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/unconstrained/Person.hbm.xml"
)
@SessionFactory
public class UnconstrainedTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person p = new Person( "gavin" );
					p.setEmployeeId( "123456" );
					session.persist( p );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUnconstrainedNoCache(SessionFactoryScope scope) {

		final CacheImplementor cache = scope.getSessionFactory().getCache();
		cache.evictEntityData( Person.class );

		scope.inTransaction(
				session -> {
					Person p = session.get( Person.class, "gavin" );
					assertNull( p.getEmployee() );
					p.setEmployee( new Employee( "123456" ) );
				}
		);

		cache.evictEntityData( Person.class );

		scope.inTransaction(
				session -> {
					Person p = session.get( Person.class, "gavin" );
					assertTrue( Hibernate.isInitialized( p.getEmployee() ) );
					assertNotNull( p.getEmployee() );
					session.remove( p );
				}
		);
	}

	@Test
	public void testUnconstrainedOuterJoinFetch(SessionFactoryScope scope) {

		final CacheImplementor cache = scope.getSessionFactory().getCache();

		cache.evictEntityData( Person.class );

		scope.inTransaction(
				session -> {
					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
					Root<Person> root = criteria.from( Person.class );
					root.fetch( "employee", JoinType.LEFT );
					criteria.where( criteriaBuilder.equal( root.get( "name" ), "gavin" ) );
					Person p = session.createQuery( criteria ).uniqueResult();
//					Person p = session.createCriteria( Person.class )
//							.setFetchMode( "employee", FetchMode.JOIN )
//							.add( Restrictions.idEq( "gavin" ) )
//							.uniqueResult();
					assertNull( p.getEmployee() );
					p.setEmployee( new Employee( "123456" ) );
				}
		);

		cache.evictEntityData( Person.class );

		scope.inTransaction(
				session -> {
					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
					Root<Person> root = criteria.from( Person.class );
					root.fetch( "employee", JoinType.LEFT );
					criteria.where( criteriaBuilder.equal( root.get( "name" ), "gavin" ) );
					Person p = session.createQuery( criteria ).uniqueResult();

//					Person p = session.createCriteria( Person.class )
//							.setFetchMode( "employee", FetchMode.JOIN )
//							.add( Restrictions.idEq( "gavin" ) )
//							.uniqueResult();
					assertTrue( Hibernate.isInitialized( p.getEmployee() ) );
					assertNotNull( p.getEmployee() );
					session.remove( p );
				}
		);
	}

	@Test
	public void testUnconstrained(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					Person p = session.get( Person.class, "gavin" );
					assertNull( p.getEmployee() );
					p.setEmployee( new Employee( "123456" ) );
				}
		);

		scope.inTransaction(
				session -> {
					Person p = session.get( Person.class, "gavin" );
					assertTrue( Hibernate.isInitialized( p.getEmployee() ) );
					assertNotNull( p.getEmployee() );
					session.remove( p );
				}
		);
	}

}
