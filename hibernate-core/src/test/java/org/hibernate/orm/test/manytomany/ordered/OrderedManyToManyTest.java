/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomany.ordered;

import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/manytomany/ordered/UserGroup.hbm.xml"
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "false")
)
public class OrderedManyToManyTest {

	@Test
	public void testManyToManyOrdering(SessionFactoryScope scope) {
		User gavin = new User( "gavin", "jboss" );
		User steve = new User( "steve", "jboss" );
		User max = new User( "max", "jboss" );
		User emmanuel = new User( "emmanuel", "jboss" );
		Group hibernate = new Group( "hibernate", "jboss" );
		scope.inTransaction(
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
		scope.inTransaction(
				s -> {
					Group h = s.get( Group.class, hibernate.getId() );
					assertFalse( Hibernate.isInitialized( h.getUsers() ) );
					assertEquals( 4, h.getUsers().size() );
					assertOrdering( h.getUsers() );
				}
		);

		// HQL (non eager)
		scope.inTransaction(
				s -> {
					Group h = (Group) s.createQuery( "from Group" ).uniqueResult();
					assertFalse( Hibernate.isInitialized( h.getUsers() ) );
					assertEquals( 4, h.getUsers().size() );
					assertOrdering( h.getUsers() );
				}
		);


		// HQL (eager)
		scope.inTransaction(
				s -> {
					Group h = (Group) s.createQuery( "from Group g inner join fetch g.users" ).uniqueResult();
					assertTrue( Hibernate.isInitialized( h.getUsers() ) );
					assertEquals( 4, h.getUsers().size() );
					assertOrdering( h.getUsers() );
				}
		);

		// criteria load (forced eager fetch)
		scope.inTransaction(
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
		scope.inTransaction(
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
		scope.inTransaction(
				s -> {
					s.remove( gavin );
					s.remove( steve );
					s.remove( max );
					s.remove( emmanuel );
					s.remove( hibernate );
				}
		);
	}

	private void assertOrdering(List users) {
		User user = extractUser( users, 0 );
		assertTrue( user.getName().equals( "emmanuel" ), "many-to-many ordering not applied" );
		user = extractUser( users, 1 );
		assertTrue( user.getName().equals( "gavin" ), "many-to-many ordering not applied" );
		user = extractUser( users, 2 );
		assertTrue( user.getName().equals( "max" ), "many-to-many ordering not applied" );
		user = extractUser( users, 3 );
		assertTrue( user.getName().equals( "steve" ), "many-to-many ordering not applied" );
	}

	private User extractUser(List users, int position) {
		return (User) users.get( position );
	}

}
