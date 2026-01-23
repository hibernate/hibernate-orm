/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
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

@DomainModel(annotatedClasses = FinalEmbeddableFieldTest.EntityWithFinalField.class)
@SessionFactory(useCollectingStatementInspector = true)
public class FinalEmbeddableFieldTest {

	@Test
	public void finalFieldNotUpdatable(SessionFactoryScope scope) {
		var statementInspector = scope.getCollectingStatementInspector();

		var persistedEntity = new EntityWithFinalField( new EmbeddableWithFinalField( "foo", "foo".toCharArray() ) );
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

		var persistedEntity = new EntityWithFinalField( new EmbeddableWithFinalField( "foo", "foo".toCharArray() ) );
		persistedEntity.setName( "Some name" );
		scope.inTransaction( s -> {
			s.persist( persistedEntity );
		} );

		statementInspector.clear();

		scope.inTransaction( s -> {
			var entity = s.find( EntityWithFinalField.class, persistedEntity.getId() );
			// Modify the final field via reflection
			try {
				var field = EmbeddableWithFinalField.class.getDeclaredField( "immutable" );
				field.setAccessible( true );
				field.set( entity.embeddable, "bar" );
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
			entity.embeddable.mutable[0] = 'b';
			entity.embeddable.mutable[1] = 'a';
			entity.embeddable.mutable[2] = 'r';
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

		@Embedded
		private final EmbeddableWithFinalField embeddable;

		protected EntityWithFinalField() {
			this.embeddable = null;
		}

		public EntityWithFinalField(EmbeddableWithFinalField embeddable) {
			this.embeddable = embeddable;
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

		public EmbeddableWithFinalField getEmbeddable() {
			return embeddable;
		}
	}

	@Embeddable
	public static class EmbeddableWithFinalField {
		private final String immutable;
		private final char[] mutable;

		protected EmbeddableWithFinalField() {
			this.immutable = null;
			this.mutable = null;
		}

		public EmbeddableWithFinalField(String immutable, char[] mutable) {
			this.immutable = immutable;
			this.mutable = mutable;
		}
	}
}
