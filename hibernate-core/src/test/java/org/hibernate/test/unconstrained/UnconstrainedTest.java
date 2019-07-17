/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.unconstrained;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.hibernate.Hibernate;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class UnconstrainedTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "unconstrained/Person.hbm.xml" };
	}

	@Test
	public void testUnconstrainedNoCache() {
		inTransaction(
				session -> {
					Person p = new Person( "gavin" );
					p.setEmployeeId( "123456" );
					session.persist( p );
				}
		);

		sessionFactory().getCache().evictEntityRegion( Person.class );

		inTransaction(
				session -> {
					Person p = session.get( Person.class, "gavin" );
					assertNull( p.getEmployee() );
					p.setEmployee( new Employee( "123456" ) );
				}
		);

		sessionFactory().getCache().evictEntityRegion( Person.class );

		inTransaction(
				session -> {
					Person p = session.get( Person.class, "gavin" );
					assertTrue( Hibernate.isInitialized( p.getEmployee() ) );
					assertNotNull( p.getEmployee() );
					session.delete( p );
				}
		);
	}

	@Test
	public void testUnconstrainedOuterJoinFetch() {
		inTransaction(
				session -> {
					Person p = new Person( "gavin" );
					p.setEmployeeId( "123456" );
					session.persist( p );
				}
		);

		sessionFactory().getCache().evictEntityRegion( Person.class );

		inTransaction(
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


		sessionFactory().getCache().evictEntityRegion( Person.class );

		inTransaction(
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
					session.delete( p );
				}
		);
	}

	@Test
	public void testUnconstrained() {
		inTransaction(
				session -> {
					Person p = new Person( "gavin" );
					p.setEmployeeId( "123456" );
					session.persist( p );
				}
		);

		inTransaction(
				session -> {
					Person p = session.get( Person.class, "gavin" );
					assertNull( p.getEmployee() );
					p.setEmployee( new Employee( "123456" ) );
				}
		);

		inTransaction(
				session -> {
					Person p = session.get( Person.class, "gavin" );
					assertTrue( Hibernate.isInitialized( p.getEmployee() ) );
					assertNotNull( p.getEmployee() );
					session.delete( p );
				}
		);
	}

}
