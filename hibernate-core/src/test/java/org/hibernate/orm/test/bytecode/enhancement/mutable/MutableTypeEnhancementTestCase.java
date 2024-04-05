/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.orm.test.bytecode.enhancement.mutable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@DomainModel(
		annotatedClasses = {
				TestEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class MutableTypeEnhancementTestCase {

	@Test
	@JiraKey("HHH-14329")
	public void testMutateMutableTypeObject(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( entityManager -> {
			TestEntity e = new TestEntity();
			e.setId( 1L );
			e.setDate( new Date() );
			e.getTexts().put( "a", "abc" );
			entityManager.persist( e );
		} );

		scope.inTransaction( entityManager -> {
			TestEntity e = entityManager.find( TestEntity.class, 1L );
			e.getDate().setTime( 0 );
			e.getTexts().put( "a", "def" );
		} );

		scope.inTransaction( entityManager -> {
			TestEntity e = entityManager.find( TestEntity.class, 1L );
			assertEquals( 0L, e.getDate().getTime() );
			assertEquals( "def", e.getTexts().get( "a" ) );
		} );
	}
}
