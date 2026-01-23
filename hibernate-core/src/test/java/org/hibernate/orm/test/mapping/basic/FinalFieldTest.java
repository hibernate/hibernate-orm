/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = FinalFieldTest.EntityWithFinalField.class)
@SessionFactory(useCollectingStatementInspector = true)
public class FinalFieldTest {

	@Test
	public void finalFieldNotUpdatable(SessionFactoryScope scope) {
		var statementInspector = scope.getCollectingStatementInspector();

		var persistedEntity = new EntityWithFinalField( "foo", "foo".toCharArray() );
		persistedEntity.setName( "Some name" );
		scope.inTransaction( s -> {
			s.persist( persistedEntity );
		} );

		statementInspector.clear();

		scope.inTransaction( s -> {
			var entity = s.find( EntityWithFinalField.class, persistedEntity.getId() );
			entity.setName( "Updated name" );
		} );

		assertEquals( 2, statementInspector.getSqlQueries().size() );
		String select = statementInspector.getSqlQueries().get( 0 );
		String update = statementInspector.getSqlQueries().get( 1 );
		assertTrue( select.startsWith( "select" ) );
		assertTrue( update.startsWith( "update" ) );
		assertFalse( update.contains( "immutable" ) );
		assertTrue( update.contains( "mutable" ) );
	}

	@Test
	public void finalFieldNotDirtyChecked(SessionFactoryScope scope) {
		var statementInspector = scope.getCollectingStatementInspector();

		var persistedEntity = new EntityWithFinalField( "foo", "foo".toCharArray() );
		persistedEntity.setName( "Some name" );
		scope.inTransaction( s -> {
			s.persist( persistedEntity );
		} );

		statementInspector.clear();

		scope.inTransaction( s -> {
			var entity = s.find( EntityWithFinalField.class, persistedEntity.getId() );
			// Modify the final field via reflection
			try {
				var field = EntityWithFinalField.class.getDeclaredField( "immutable" );
				field.setAccessible( true );
				field.set( entity, "bar" );
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		} );

		assertEquals( 1, statementInspector.getSqlQueries().size() );
		assertTrue( statementInspector.getSqlQueries().get( 0 ).startsWith( "select" ) );

		statementInspector.clear();

		scope.inTransaction( s -> {
			var entity = s.find( EntityWithFinalField.class, persistedEntity.getId() );
			entity.mutable[0] = 'b';
			entity.mutable[1] = 'a';
			entity.mutable[2] = 'r';
		} );

		assertEquals( 2, statementInspector.getSqlQueries().size() );
		assertTrue( statementInspector.getSqlQueries().get( 0 ).startsWith( "select" ) );
		assertTrue( statementInspector.getSqlQueries().get( 1 ).startsWith( "update" ) );
	}

	@Entity(name = "EntityWithFinalField")
	public static class EntityWithFinalField {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private final String immutable;

		private final char[] mutable;

		protected EntityWithFinalField() {
			this.immutable = null;
			this.mutable = null;
		}

		public EntityWithFinalField(String immutable, char[] mutable) {
			this.immutable = immutable;
			this.mutable = mutable;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getImmutable() {
			return immutable;
		}
	}
}
