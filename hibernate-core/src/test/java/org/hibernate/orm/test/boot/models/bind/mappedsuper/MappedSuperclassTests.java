/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind.mappedsuper;

import java.util.Set;

import org.hibernate.annotations.TenantId;
import org.hibernate.orm.test.boot.models.bind.callbacks.HierarchyRoot;
import org.hibernate.orm.test.boot.models.bind.callbacks.HierarchySuper;
import org.hibernate.boot.models.categorize.spi.BasicKeyMapping;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.orm.test.boot.models.bind.BindingTestingHelper;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class MappedSuperclassTests {

	@Test
	void testAssumptions() {
		final Set<EntityHierarchy> entityHierarchies = BindingTestingHelper.buildHierarchyMetadata( HierarchyRoot.class, HierarchySuper.class );
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();

		assertThat( entityHierarchy.getIdMapping() ).isNotNull();
		final BasicKeyMapping idMapping = (BasicKeyMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getAttribute().getMember().getAnnotationUsage( Id.class ) ).isNotNull();
		assertThat( idMapping.getAttribute().getMember().getAnnotationUsage( EmbeddedId.class ) ).isNull();

		assertThat( entityHierarchy.getVersionAttribute() ).isNotNull();
		assertThat( entityHierarchy.getVersionAttribute().getMember().getAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getAnnotationUsage( TenantId.class ) ).isNotNull();

		assertThat( entityHierarchy.getCacheRegion() ).isNotNull();
		assertThat( entityHierarchy.getCacheRegion().getAccessType() ).isEqualTo( AccessType.READ_ONLY );

		assertThat( entityHierarchy.getInheritanceType() ).isNotNull();
		assertThat( entityHierarchy.getInheritanceType() ).isEqualTo( InheritanceType.JOINED );
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Test
	@ServiceRegistry
	void testBindings(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final MappedSuperclass superBinding = metadataCollector.getMappedSuperclass( HierarchySuper.class );
					final PersistentClass rootBinding = metadataCollector.getEntityBinding( HierarchyRoot.class.getName() );

					// todo - https://github.com/sebersole/hibernate-models2/issues/81
//					assertThat( superBinding.getMappedClass() ).isEqualTo( HierarchySuper.class );
//					assertThat( superBinding.getImplicitTable() ).isNull();
//					assertThat( superBinding.hasIdentifierProperty() ).isFalse();

					assertThat( rootBinding.getMappedClass() ).isEqualTo( HierarchyRoot.class );
					assertThat( rootBinding.getSuperMappedSuperclass() ).isSameAs( superBinding );
					assertThat( rootBinding.getImplicitTable() ).isNotNull();
					assertThat( rootBinding.getTable() ).isNotNull();
					assertThat( rootBinding.getRootTable() ).isSameAs( rootBinding.getTable() );
					assertThat( rootBinding.getIdentityTable() ).isSameAs( rootBinding.getTable() );
					assertThat( rootBinding.getIdentityTable().getPrimaryKey() ).isNotNull();
					assertThat( rootBinding.getIdentityTable().getPrimaryKey().getColumns() ).hasSize( 1 );
					assertThat( rootBinding.getIdentifier() ).isNotNull();
					assertThat( rootBinding.getIdentifierProperty() ).isNotNull();
					assertThat( rootBinding.getIdentifierProperty().getColumns() ).hasSize( 1 );
				},
				scope.getRegistry(),
				HierarchyRoot.class,
				HierarchySuper.class
		);
	}

}
