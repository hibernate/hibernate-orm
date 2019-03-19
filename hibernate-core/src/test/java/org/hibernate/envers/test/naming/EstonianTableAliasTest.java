/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.naming;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import ee.estonia.envers.support.domain.naming.Child;
import ee.estonia.envers.support.domain.naming.Parent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6738")
public class EstonianTableAliasTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Long parentId = null;
	private Long childId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Parent.class, Child.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction(
				entityManager -> {
					final Parent parent = new Parent( "parent" );
					final Child child = new Child( "child" );
					parent.getCollection().add( child );

					entityManager.persist( child );
					entityManager.persist( parent );

					parentId = parent.getId();
					childId = child.getId();
				}
		);
	}

	@DynamicTest
	public void testAuditChildTableAlias() {
		Parent parent = new Parent( "parent", parentId );
		Child child = new Child( "child", childId );

		Parent ver1 = getAuditReader().find( Parent.class, parentId, 1 );

		assertThat( parent, equalTo( ver1 ) );
		assertThat( ver1.getCollection(), contains( child ) );
	}
}