/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.naming;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.tools.TestTools;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import ee.estonia.entities.Child;
import ee.estonia.entities.Parent;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6738")
public class EstonianTableAlias extends BaseEnversJPAFunctionalTestCase {
	private Long parentId = null;
	private Long childId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Parent.class, Child.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1
		em.getTransaction().begin();
		Parent parent = new Parent( "parent" );
		Child child = new Child( "child" );
		parent.getCollection().add( child );
		em.persist( child );
		em.persist( parent );
		em.getTransaction().commit();

		parentId = parent.getId();
		childId = child.getId();
	}

	@Test
	public void testAuditChildTableAlias() {
		Parent parent = new Parent( "parent", parentId );
		Child child = new Child( "child", childId );

		Parent ver1 = getAuditReader().find( Parent.class, parentId, 1 );

		Assert.assertEquals( parent, ver1 );
		Assert.assertEquals( TestTools.makeSet( child ), ver1.getCollection() );
	}
}