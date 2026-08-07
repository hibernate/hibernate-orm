/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import java.lang.reflect.Modifier;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cascade.spi.CascadePoint;
import org.hibernate.cascade.spi.CascadeStyle;
import org.hibernate.cascade.spi.CascadeStyles;
import org.hibernate.cascade.spi.CascadingAction;
import org.hibernate.cascade.spi.CascadingActions;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/// Compatibility tests for removal of the historical cascade extension points.
///
/// @author Steve Ebersole
class CascadeExtensionRemovalTest {
	@Test
	void extensionBaseClassesAndStyleRegistrationAreNotPublic() throws ClassNotFoundException {
		assertThat( CascadeStyle.class ).hasAnnotation( Internal.class );
		assertThat( CascadingAction.class ).hasAnnotation( Internal.class );
		assertThat( CascadeStyles.class.getDeclaredMethods() )
				.noneMatch( method -> method.getName().equals( "registerCascadeStyle" ) );
		assertThat( Modifier.isPublic( Class.forName(
				"org.hibernate.cascade.spi.CascadeStyles$BaseCascadeStyle"
		).getModifiers() ) ).isFalse();
		assertThat( Modifier.isPublic( Class.forName(
				"org.hibernate.cascade.spi.CascadingActions$BaseCascadingAction"
		).getModifiers() ) ).isFalse();
	}

	@Test
	void unsupportedNamedStyleIsRejectedAtSessionFactoryBootstrap() {
		final StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		try {
			assertThatThrownBy( () -> new MetadataSources( serviceRegistry )
					.addResource( "org/hibernate/cascade/internal/extension-cascade.hbm.xml" )
					.buildMetadata()
					.buildSessionFactory() )
					.isInstanceOf( MappingException.class )
					.hasMessageContaining( "Unsupported cascade style: external-style" );
		}
		finally {
			serviceRegistry.close();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void customActionIsRejectedBeforeTraversal() {
		final CascadingAction<Object> customAction = mock( CascadingAction.class );
		final EventSource session = mock( EventSource.class );
		final EntityPersister persister = mock( EntityPersister.class );

		assertThatThrownBy( () -> Cascade.cascade(
				customAction,
				CascadePoint.BEFORE_MERGE,
				session,
				persister,
				new Object(),
				null
		) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "Unsupported cascading action implementation" )
				.hasMessageContaining( customAction.getClass().getName() );

		verifyNoInteractions( session, persister );
	}

	@Test
	void everyBuiltInActionPassesTheCoordinatorBoundary() {
		final EventSource session = mock( EventSource.class );
		final EntityPersister persister = mock( EntityPersister.class );
		final Object parent = new Object();

		for ( CascadingAction<?> action : List.of(
				CascadingActions.REMOVE,
				CascadingActions.REFRESH,
				CascadingActions.EVICT,
				CascadingActions.MERGE,
				CascadingActions.PERSIST,
				CascadingActions.PERSIST_ON_FLUSH,
				CascadingActions.CHECK_ON_FLUSH ) ) {
			assertThatCode( () -> cascade( action, session, persister, parent ) ).doesNotThrowAnyException();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void cascade(
			CascadingAction<?> action,
			EventSource session,
			EntityPersister persister,
			Object parent) {
		Cascade.cascade( (CascadingAction) action, CascadePoint.BEFORE_MERGE, session, persister, parent, null );
	}

	static class Parent {
		int id;
		Child child;
	}

	static class Child {
		int id;
	}
}
