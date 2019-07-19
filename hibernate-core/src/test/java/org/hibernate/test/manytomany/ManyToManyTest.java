/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.manytomany;

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
public class ManyToManyTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "manytomany/UserGroup.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "false");
	}

	@Test
	public void testManyToManyWithFormula() {
		User user = new User("gavin", "jboss");
		Group seamGroup = new Group("seam", "jboss");
		Group hbGroup = new Group("hibernate", "jboss");
		inTransaction(
				s -> {
					user.getGroups().add(seamGroup);
					user.getGroups().add(hbGroup);
					seamGroup.getUsers().add(user);
					hbGroup.getUsers().add(user);
					s.persist(user);
					s.persist(seamGroup);
					s.persist(hbGroup);
				}
		);

		inTransaction(
				s -> {
					User gavin = s.get(User.class, user);
					assertFalse( Hibernate.isInitialized( gavin.getGroups() ) );
					assertEquals( 2, gavin.getGroups().size() );
					Group hb = s.get(Group.class, hbGroup);
					assertFalse( Hibernate.isInitialized( hb.getUsers() ) );
					assertEquals( 1, hb.getUsers().size() );
				}
		);

		inTransaction(
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

		inTransaction(
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

		inTransaction(
				s -> {
					User gavin = (User) s.createQuery("from User u join fetch u.groups g join fetch g.users").uniqueResult();
					assertTrue( Hibernate.isInitialized( gavin.getGroups() ) );
					assertEquals( 2, gavin.getGroups().size() );
					Group group = (Group) gavin.getGroups().iterator().next();
					assertTrue( Hibernate.isInitialized( group.getUsers() ) );
					assertEquals( 1, group.getUsers().size() );
				}
		);

		inTransaction(
				s -> {
					User gavin = s.get(User.class, user);
					Group hb = s.get(Group.class, hbGroup);
					gavin.getGroups().remove(hb);
				}
		);

		inTransaction(
				s -> {
					User gavin = s.get(User.class, user);
					assertEquals( gavin.getGroups().size(), 1 );
					Group hb = s.get(Group.class, hbGroup);
					assertEquals( hb.getUsers().size(), 0 );
				}
		);
		inTransaction(
				s -> {
					s.delete(user);
					s.flush();
					s.createQuery("delete from Group").executeUpdate();
				}
		);
	}
}

