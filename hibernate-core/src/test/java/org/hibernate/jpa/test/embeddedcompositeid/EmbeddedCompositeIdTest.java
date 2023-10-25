/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.embeddedcompositeid;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;

import java.util.List;

/**
 * @author Habib Zerai
 */
public class EmbeddedCompositeIdTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Role.class,
				User.class,
				UserRole.class
		};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-15931")
	public void testEmbeddedCompositeId() throws Exception {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		Role role = new Role();
		role.setIdentifier("ADMIN");
		role = entityManager.merge(role);
		entityManager.flush();

		Role roleFromDb = entityManager.find(Role.class, role.getCodeObject());
		UserRole userRole = new UserRole();
		userRole.setRole(roleFromDb);

		User user = new User();
		user.setIdentifier("hzerai");
		user.setVersion(1);
		user.addUserRole(userRole);
		userRole.setUser(user);
		entityManager.merge(user);

		entityManager.flush();
		entityManager.clear();

		TypedQuery<UserRole> query = entityManager.createQuery("select a from userRole a", UserRole.class);
		List<UserRole> r = query.getResultList();
		assertEquals(1, r.size());
		
		entityManager.getTransaction().commit();
	}
}
