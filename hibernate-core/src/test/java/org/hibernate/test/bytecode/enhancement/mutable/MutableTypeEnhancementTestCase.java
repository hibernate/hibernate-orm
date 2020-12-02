/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.test.bytecode.enhancement.mutable;

import java.util.Date;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BytecodeEnhancerRunner.class)
public class MutableTypeEnhancementTestCase extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14329")
	public void testMutateMutableTypeObject() throws Exception {
		inTransaction( entityManager -> {
			TestEntity e = new TestEntity();
			e.setId( 1L );
			e.setDate( new Date() );
			e.getTexts().put( "a", "abc" );
			entityManager.persist( e );
		} );

		inTransaction( entityManager -> {
			TestEntity e = entityManager.find( TestEntity.class, 1L );
			e.getDate().setTime( 0 );
			e.getTexts().put( "a", "def" );
		} );

		inTransaction( entityManager -> {
			TestEntity e = entityManager.find( TestEntity.class, 1L );
			Assert.assertEquals( 0L, e.getDate().getTime() );
			Assert.assertEquals( "def", e.getTexts().get( "a" ) );
		} );
	}
}
