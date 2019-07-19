/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.manytomany.ordered;

import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class OrderedManyToManyTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "manytomany/ordered/UserGroup.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false");
	}

	@Test
	public void testManyToManyOrdering() {
		User gavin = new User( "gavin", "jboss" );
		User steve = new User( "steve", "jboss" );
		User max = new User( "max", "jboss" );
		User emmanuel = new User( "emmanuel", "jboss" );
		Group hibernate = new Group( "hibernate", "jboss" );
		inTransaction(
				s -> {
					s.persist( gavin );
					s.persist( steve );
					s.persist( max );
					s.persist( emmanuel );
					hibernate.addUser( gavin );
					hibernate.addUser( steve );
					hibernate.addUser( max );
					hibernate.addUser( emmanuel );
					s.persist( hibernate );
				}
		);

		// delayed collection load...
		inTransaction(
				s -> {
					Group h = s.get( Group.class, hibernate.getId() );
					assertFalse( Hibernate.isInitialized( h.getUsers() ) );
					assertEquals( 4, h.getUsers().size() );
					assertOrdering( h.getUsers() );
				}
		);

		// HQL (non eager)
		inTransaction(
				s -> {
					Group h = (Group) s.createQuery( "from Group" ).uniqueResult();
					assertFalse( Hibernate.isInitialized( h.getUsers() ) );
					assertEquals( 4, h.getUsers().size() );
					assertOrdering( h.getUsers() );
				}
		);


		// HQL (eager)
		inTransaction(
				s -> {
					Group h = ( Group ) s.createQuery( "from Group g inner join fetch g.users" ).uniqueResult();
					assertTrue( Hibernate.isInitialized( h.getUsers() ) );
					assertEquals( 4, h.getUsers().size() );
					assertOrdering( h.getUsers() );
				}
		);

		// criteria load (forced eager fetch)
		inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Group> criteria = criteriaBuilder.createQuery( Group.class );
					criteria.from( Group.class ).fetch( "users", JoinType.LEFT );

					Group h = s.createQuery( criteria ).uniqueResult();
//					Criteria criteria = s.createCriteria( Group.class );
//					criteria.setFetchMode( "users", FetchMode.JOIN );
//					hibernate = ( Group ) criteria.uniqueResult();
					assertTrue( Hibernate.isInitialized( h.getUsers() ) );
					assertEquals( 4, h.getUsers().size() );
					assertOrdering( h.getUsers() );
				}
		);


		// criteria load (forced non eager fetch)
		inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Group> criteria = criteriaBuilder.createQuery( Group.class );
					criteria.from( Group.class ).join( "users", JoinType.LEFT );

					Group h = s.createQuery( criteria ).uniqueResult();
//					criteria = s.createCriteria( Group.class );
//					criteria.setFetchMode( "users", FetchMode.SELECT );
//					hibernate = ( Group ) criteria.uniqueResult();
					assertFalse( Hibernate.isInitialized( h.getUsers() ) );
					assertEquals( 4, h.getUsers().size() );
					assertOrdering( h.getUsers() );
				}
		);


		// clean up
		inTransaction(
				s -> {
					s.delete( gavin );
					s.delete( steve );
					s.delete( max );
					s.delete( emmanuel );
					s.delete( hibernate );
				}
		);
	}

	private void assertOrdering(List users) {
		User user = extractUser( users, 0 );
		assertTrue( "many-to-many ordering not applied", user.getName().equals( "emmanuel" ) );
		user = extractUser( users, 1 );
		assertTrue( "many-to-many ordering not applied", user.getName().equals( "gavin" ) );
		user = extractUser( users, 2 );
		assertTrue( "many-to-many ordering not applied", user.getName().equals( "max" ) );
		user = extractUser( users, 3 );
		assertTrue( "many-to-many ordering not applied", user.getName().equals( "steve" ) );
	}

	private User extractUser(List users, int position) {
		return ( User ) users.get( position );
	}

}
