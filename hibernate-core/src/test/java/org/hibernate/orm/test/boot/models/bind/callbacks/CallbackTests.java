/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind.callbacks;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.orm.test.boot.models.bind.BindingTestingHelper;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class CallbackTests {
	@Test
	@ServiceRegistry
	void testMappedSuper(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final var bindingState = context.getBindingState();
					final var metadataCollector = context.getMetadataCollector();

					final MappedSuperclass mappedSuper = metadataCollector.getMappedSuperclass( HierarchySuper.class );
					assertThat( mappedSuper ).isNotNull();

					final PersistentClass entityBinding = metadataCollector.getEntityBinding( HierarchyRoot.class.getName() );
					assertThat( entityBinding ).isNotNull();
					assertThat( entityBinding.getSuperPersistentClass() ).isNull();
					assertThat( entityBinding.getSuperType() ).isEqualTo( mappedSuper );
					assertThat( entityBinding.getSuperMappedSuperclass() ).isEqualTo( mappedSuper );
					assertThat( entityBinding.getCallbackDefinitions() ).hasSize( 3 );
				},
				scope.getRegistry(),
				HierarchySuper.class,
				HierarchyRoot.class
		);
	}

	@Test
	void testSimpleEventListenerResolution() {
		final CategorizedDomainModel categorizedDomainModel = BindingTestingHelper.buildCategorizedDomainModel(
				HierarchyRoot.class,
				HierarchySuper.class
		);
		final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
		final EntityHierarchy hierarchy = entityHierarchies.iterator().next();

		final EntityTypeMetadata rootMapping = hierarchy.getRoot();
		assertThat( rootMapping.getHierarchyJpaEventListeners() ).hasSize( 3 );
		final List<String> listenerClassNames = rootMapping.getHierarchyJpaEventListeners()
				.stream()
				.map( listener -> listener.getCallbackClass().getClassName() )
				.collect( Collectors.toList() );
		assertThat( listenerClassNames ).containsExactly(
				Listener1.class.getName(),
				Listener2.class.getName(),
				HierarchyRoot.class.getName()
		);

		final IdentifiableTypeMetadata superMapping = rootMapping.getSuperType();
		assertThat( superMapping.getHierarchyJpaEventListeners() ).hasSize( 1 );
		final String callbackClassName = superMapping.getHierarchyJpaEventListeners()
				.get( 0 )
				.getCallbackClass()
				.getClassName();
		assertThat( callbackClassName ).isEqualTo( Listener1.class.getName() );
	}
}
