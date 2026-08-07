/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cascade.spi.CascadeStyle;
import org.hibernate.cascade.spi.CascadingAction;
import org.hibernate.cascade.spi.CascadePoint;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests invocation-local cascade traversal state and exceptional restoration.
///
/// @author Steve Ebersole
class CascadeTraversalContextTest {
	@Test
	void basicStateAndPathLifecycle() {
		final var action = action();
		final var session = mock( EventSource.class );
		final var persister = mock( EntityPersister.class );
		final var root = new Object();
		final var actionContext = new Object();
		final var context = new CascadeTraversalContext<>(
				action,
				CascadePoint.BEFORE_FLUSH,
				session,
				persister,
				root,
				actionContext
		);

		assertThat( context.action() ).isSameAs( action );
		assertThat( context.cascadePoint() ).isEqualTo( CascadePoint.BEFORE_FLUSH );
		assertThat( context.session() ).isSameAs( session );
		assertThat( context.rootPersister() ).isSameAs( persister );
		assertThat( context.root() ).isSameAs( root );
		assertThat( context.actionContext() ).isSameAs( actionContext );
		assertThat( context.currentParent() ).isSameAs( root );
		assertThat( context.path() ).isNull();

		context.pushPath( "address" );
		context.pushPath( "country" );
		assertThat( context.path() ).containsExactly( "address", "country" );

		context.popPath();
		assertThat( context.path() ).containsExactly( "address" );
		context.popPath();
		assertThat( context.path() ).isNull();

		assertThat( context.changeCascadePoint( CascadePoint.AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION ) )
				.isEqualTo( CascadePoint.BEFORE_FLUSH );
		assertThat( context.changeCascadePoint( CascadePoint.BEFORE_FLUSH ) )
				.isEqualTo( CascadePoint.AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION );
	}

	@Test
	void componentFailureRestoresTraversalAndPersistenceContextState() {
		final var action = action();
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var persister = mock( EntityPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var componentType = mock( ComponentType.class );
		final var entityType = mock( EntityType.class );
		final var componentStyle = mock( CascadeStyle.class );
		final var associationStyle = mock( CascadeStyle.class );
		final var root = new Object();
		final var component = new Object();
		final var child = new Object();
		final var actionContext = new Object();
		final var failure = new HibernateException( "expected failure" );

		when( session.getFactory() ).thenReturn( factory );
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( persister.getEntityName() ).thenReturn( "Root" );
		when( persister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( persister.getPropertyTypes() ).thenReturn( new ComponentType[] { componentType } );
		when( persister.getPropertyNames() ).thenReturn( new String[] { "component" } );
		when( persister.getPropertyCascadeStyles() ).thenReturn( new CascadeStyle[] { componentStyle } );
		when( persister.getValue( root, 0 ) ).thenReturn( component );
		when( action.anythingToCascade( persister ) ).thenReturn( true );
		when( action.appliesTo( componentType, componentStyle ) ).thenReturn( true );

		when( componentType.getSubtypes() ).thenReturn( new EntityType[] { entityType } );
		when( componentType.getPropertyNames() ).thenReturn( new String[] { "association" } );
		when( componentType.getCascadeStyle( 0 ) ).thenReturn( associationStyle );
		when( componentType.getPropertyValues( component, session ) ).thenReturn( new Object[] { child } );
		when( action.appliesTo( entityType, associationStyle ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.BEFORE_MERGE, entityType, factory ) ).thenReturn( true );
		when( associationStyle.reallyDoCascade( action ) ).thenReturn( true );
		when( entityType.getAssociatedEntityName() ).thenReturn( "Child" );
		doThrow( failure ).when( action ).cascade(
				same( session ),
				same( child ),
				eq( "Child" ),
				eq( "Root" ),
				eq( "association" ),
				eq( List.of( "component" ) ),
				same( actionContext ),
				eq( false )
		);

		final var context = new CascadeTraversalContext<>(
				action,
				CascadePoint.BEFORE_MERGE,
				session,
				persister,
				root,
				actionContext
		);

		assertThatThrownBy( () -> Cascade.cascade( context ) ).isSameAs( failure );

		assertRestoredRootState( context, root, CascadePoint.BEFORE_MERGE );
		verify( persistenceContext ).removeChildParent( child );
	}

	@Test
	void collectionFailureRestoresTranslatedCascadePoint() {
		final var action = action();
		final var session = mock( EventSource.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var mappingMetamodel = mock( MappingMetamodelImplementor.class );
		final var persistenceContext = mock( PersistenceContext.class );
		final var entityPersister = mock( EntityPersister.class );
		final var collectionPersister = mock( CollectionPersister.class );
		final var enhancementMetadata = mock( BytecodeEnhancementMetadata.class );
		final var collectionType = mock( CollectionType.class );
		final var elementType = mock( EntityType.class );
		final var style = mock( CascadeStyle.class );
		final var root = new Object();
		final var collection = new Object();
		final var child = new Object();
		final var failure = new HibernateException( "expected failure" );

		when( session.getFactory() ).thenReturn( factory );
		when( session.getPersistenceContextInternal() ).thenReturn( persistenceContext );
		when( factory.getMappingMetamodel() ).thenReturn( mappingMetamodel );
		when( entityPersister.getEntityName() ).thenReturn( "Root" );
		when( entityPersister.getBytecodeEnhancementMetadata() ).thenReturn( enhancementMetadata );
		when( entityPersister.getPropertyTypes() ).thenReturn( new CollectionType[] { collectionType } );
		when( entityPersister.getPropertyNames() ).thenReturn( new String[] { "children" } );
		when( entityPersister.getPropertyCascadeStyles() ).thenReturn( new CascadeStyle[] { style } );
		when( entityPersister.getValue( root, 0 ) ).thenReturn( collection );
		when( collectionType.getRole() ).thenReturn( "Root.children" );
		when( mappingMetamodel.getCollectionDescriptor( "Root.children" ) ).thenReturn( collectionPersister );
		when( collectionPersister.getElementType() ).thenReturn( elementType );
		when( action.anythingToCascade( entityPersister ) ).thenReturn( true );
		when( action.appliesTo( collectionType, style ) ).thenReturn( true );
		when( action.cascadeNow( CascadePoint.AFTER_INSERT_BEFORE_DELETE, collectionType, factory ) ).thenReturn( true );
		when( style.reallyDoCascade( action ) ).thenReturn( true );
		doReturn( singleton( child ).iterator() )
				.when( action ).getCascadableChildrenIterator( session, collectionType, collection );
		when( action.cascadeNow(
				CascadePoint.AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION,
				elementType,
				factory
		) ).thenReturn( true );
		when( elementType.getAssociatedEntityName() ).thenReturn( "Child" );
		doThrow( failure ).when( action ).cascade(
				same( session ),
				same( child ),
				eq( "Child" ),
				eq( "Root" ),
				eq( "children" ),
				isNull(),
				isNull(),
				eq( false )
		);

		final var context = new CascadeTraversalContext<>(
				action,
				CascadePoint.AFTER_INSERT_BEFORE_DELETE,
				session,
				entityPersister,
				root,
				null
		);

		assertThatThrownBy( () -> Cascade.cascade( context ) ).isSameAs( failure );

		assertRestoredRootState( context, root, CascadePoint.AFTER_INSERT_BEFORE_DELETE );
		verify( action ).cascadeNow(
				CascadePoint.AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION,
				elementType,
				factory
		);
		verify( persistenceContext ).removeChildParent( child );
	}

	private static void assertRestoredRootState(
			CascadeTraversalContext<?> context,
			Object root,
			CascadePoint cascadePoint) {
		assertThat( context.cascadePoint() ).isEqualTo( cascadePoint );
		assertThat( context.currentParent() ).isSameAs( root );
		assertThat( context.currentPropertyName() ).isNull();
		assertThat( context.currentPropertyType() ).isNull();
		assertThat( context.currentCascadeStyle() ).isNull();
		assertThat( context.currentPropertyIndex() ).isEqualTo( -1 );
		assertThat( context.path() ).isNull();
	}

	@SuppressWarnings("unchecked")
	private static CascadingAction<Object> action() {
		return mock( CascadingAction.class );
	}
}
