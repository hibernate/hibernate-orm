/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomany;

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
		xmlMappings = "org/hibernate/orm/test/manytomany/UserGroup.hbm.xml"
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "false")
)
public class ManyToManyTest {

	@Test
	public void testManyToManyWithFormula(SessionFactoryScope scope) {
		User user = new User( "gavin", "jboss" );
		Group seamGroup = new Group( "seam", "jboss" );
		Group hbGroup = new Group( "hibernate", "jboss" );
		scope.inTransaction(
				s -> {
					user.getGroups().add( seamGroup );
					user.getGroups().add( hbGroup );
					seamGroup.getUsers().add( user );
					hbGroup.getUsers().add( user );
					s.persist( user );
					s.persist( seamGroup );
					s.persist( hbGroup );
				}
		);

		scope.inTransaction(
				s -> {
					User gavin = s.get( User.class, user );
					assertFalse( Hibernate.isInitialized( gavin.getGroups() ) );
					assertEquals( 2, gavin.getGroups().size() );
					Group hb = s.get( Group.class, hbGroup );
					assertFalse( Hibernate.isInitialized( hb.getUsers() ) );
					assertEquals( 1, hb.getUsers().size() );
				}
		);

		scope.inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<User> criteria = criteriaBuilder.createQuery( User.class );
					criteria.from( User.class ).fetch( "groups", JoinType.LEFT );

					User gavin = s.createQuery( criteria ).uniqueResult();
//					User gavin = (User) s.createCriteria(User.class)
//							.setFetchMode("groups", FetchMode.JOIN)
//							.uniqueResult();
					assertTrue( Hibernate.isInitialized( gavin.getGroups() ) );
					assertEquals( 2, gavin.getGroups().size() );
					Group group = (Group) gavin.getGroups().iterator().next();
					assertFalse( Hibernate.isInitialized( group.getUsers() ) );
					assertEquals( 1, group.getUsers().size() );

				}
		);

		scope.inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<User> criteria = criteriaBuilder.createQuery( User.class );
					criteria.from( User.class ).fetch( "groups", JoinType.LEFT ).fetch( "users", JoinType.LEFT );

					User gavin = s.createQuery( criteria ).uniqueResult();
//					User gavin = (User) s.createCriteria(User.class)
//							.setFetchMode("groups", FetchMode.JOIN)
//							.setFetchMode("groups.users", FetchMode.JOIN)
//							.uniqueResult();
					assertTrue( Hibernate.isInitialized( gavin.getGroups() ) );
					assertEquals( 2, gavin.getGroups().size() );
					Group group = (Group) gavin.getGroups().iterator().next();
					assertTrue( Hibernate.isInitialized( group.getUsers() ) );
					assertEquals( 1, group.getUsers().size() );

				}
		);

		scope.inTransaction(
				s -> {
					User gavin = (User) s.createQuery( "from User u join fetch u.groups g join fetch g.users" )
							.uniqueResult();
					assertTrue( Hibernate.isInitialized( gavin.getGroups() ) );
					assertEquals( 2, gavin.getGroups().size() );
					Group group = (Group) gavin.getGroups().iterator().next();
					assertTrue( Hibernate.isInitialized( group.getUsers() ) );
					assertEquals( 1, group.getUsers().size() );
				}
		);

		scope.inTransaction(
				s -> {
					User gavin = s.get( User.class, user );
					Group hb = s.get( Group.class, hbGroup );
					gavin.getGroups().remove( hb );
				}
		);

		scope.inTransaction(
				s -> {
					User gavin = s.get( User.class, user );
					assertEquals( 1, gavin.getGroups().size() );
					Group hb = s.get( Group.class, hbGroup );
					assertEquals( 0, hb.getUsers().size() );
				}
		);

		scope.inTransaction(
				s -> {
					s.remove( user );
					s.flush();
					s.createQuery( "delete from Group" ).executeUpdate();
				}
		);
	}
}
