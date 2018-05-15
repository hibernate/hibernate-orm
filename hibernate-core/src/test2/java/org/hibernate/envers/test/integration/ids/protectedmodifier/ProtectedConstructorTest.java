/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.ids.protectedmodifier;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7934")
public class ProtectedConstructorTest extends BaseEnversJPAFunctionalTestCase {
	private final ProtectedConstructorEntity testEntity = new ProtectedConstructorEntity(
			new WrappedStringId(
					"embeddedStringId"
			), "string"
	);

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {WrappedStringId.class, ProtectedConstructorEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		em.persist( testEntity );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testAuditEntityInstantiation() {
		List result = getAuditReader().createQuery()
				.forEntitiesAtRevision( ProtectedConstructorEntity.class, 1 )
				.getResultList();
		Assert.assertEquals( Arrays.asList( testEntity ), result );
	}
}
