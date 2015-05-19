/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embedded;

import org.hibernate.Session;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
@FailureExpected(jiraKey="HHH-8021")
public class NestedEmbeddableAttributeOverrideTest extends BaseCoreFunctionalTestCase {
	
	@Test
	@TestForIssue(jiraKey="HHH-8021")
	public void testAttributeOverride() {
		EmbeddableB embedB = new EmbeddableB();
		embedB.setEmbedAttrB( "B" );

		EmbeddableA embedA = new EmbeddableA();
		embedA.setEmbedAttrA("A");
		embedA.setEmbedB(embedB);
		
		EntityWithNestedEmbeddables entity = new EntityWithNestedEmbeddables();
		entity.setEmbedA(embedA);
		
		Session s = openSession();
		s.beginTransaction();
		s.persist( entity );
		s.getTransaction().commit();
		s.close();
	}
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityWithNestedEmbeddables.class };
	}
}
