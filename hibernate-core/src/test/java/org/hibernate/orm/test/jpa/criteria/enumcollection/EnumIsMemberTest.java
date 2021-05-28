/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.enumcollection;

import java.util.List;
import java.util.Set;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * Mote that these are simply performing syntax checking (can the criteria query
 * be properly compiled and executed)
 *
 * @author Steve Ebersole
 */
@Jpa( annotatedClasses = User.class )
public class EnumIsMemberTest {

	@BeforeEach
	void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			User user = new User();
			user.setId( 1L );
			user.getRoles().add( User.Role.Admin );
			em.persist( user );
		} );
	}

	@AfterEach
	void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> em.createQuery( "delete User" ).executeUpdate() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9605")
	void testQueryEnumCollection(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();

			CriteriaQuery<User> query = builder.createQuery( User.class );
			Root<User> root = query.from( User.class );

			Expression<Set<User.Role>> roles = root.get( User_.roles );

			// Using the correct collection of enums and an enum parameter
			query.where( builder.isMember( User.Role.Admin, roles ) );

			TypedQuery<User> typedQuery = em.createQuery( query );
			List<User> users = typedQuery.getResultList();
			assertThat( users, hasSize( 1 ) );
		} );
	}

}
