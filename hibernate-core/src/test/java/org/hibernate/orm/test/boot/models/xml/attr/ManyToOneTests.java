/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.attr;

import java.util.List;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.Target;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.FieldDetails;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor.processManagedResources;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class ManyToOneTests {
	@Test
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testSimpleManyToOne(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/attr/many-to-one/simple.xml" )
				.build();

		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl(
				serviceRegistry,
				new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry )
		);
		final CategorizedDomainModel categorizedDomainModel = processManagedResources(
				managedResources,
				bootstrapContext
		);

		assertThat( categorizedDomainModel.getEntityHierarchies() ).hasSize( 1 );

		final EntityHierarchy hierarchy = categorizedDomainModel.getEntityHierarchies().iterator().next();
		final EntityTypeMetadata root = hierarchy.getRoot();

		final FieldDetails parentField = root.getClassDetails().findFieldByName( "parent" );
		final AnnotationUsage<ManyToOne> manyToOneAnn = parentField.getAnnotationUsage( ManyToOne.class );
		assertThat( manyToOneAnn ).isNotNull();
		final AnnotationUsage<JoinColumn> joinColumnAnn = parentField.getAnnotationUsage( JoinColumn.class );
		assertThat( joinColumnAnn ).isNotNull();
		assertThat( joinColumnAnn.getString( "name" ) ).isEqualTo( "parent_fk" );

		assertThat( parentField.getAnnotationUsage( JoinColumn.class ) ).isNotNull();

		final AnnotationUsage<NotFound> notFoundAnn = parentField.getAnnotationUsage( NotFound.class );
		assertThat( notFoundAnn ).isNotNull();
		assertThat( notFoundAnn.<NotFoundAction>getEnum( "action" ) ).isEqualTo( NotFoundAction.IGNORE );

		final AnnotationUsage<OnDelete> onDeleteAnn = parentField.getAnnotationUsage( OnDelete.class );
		assertThat( onDeleteAnn ).isNotNull();
		assertThat( onDeleteAnn.<OnDeleteAction>getEnum( "action" ) ).isEqualTo( OnDeleteAction.CASCADE );

		final AnnotationUsage<Fetch> fetchAnn = parentField.getAnnotationUsage( Fetch.class );
		assertThat( fetchAnn ).isNotNull();
		assertThat( fetchAnn.<FetchMode>getEnum( "value" ) ).isEqualTo( FetchMode.SELECT );

		final AnnotationUsage<OptimisticLock> optLockAnn = parentField.getAnnotationUsage( OptimisticLock.class );
		assertThat( optLockAnn ).isNotNull();
		assertThat( optLockAnn.getBoolean( "excluded" ) ).isTrue();

		final AnnotationUsage<Target> targetAnn = parentField.getAnnotationUsage( Target.class );
		assertThat( targetAnn ).isNotNull();
		assertThat( targetAnn.getString( "value" ) ).isEqualTo( "org.hibernate.orm.test.boot.models.xml.attr.ManyToOneTests$SimpleEntity" );

		final AnnotationUsage<Cascade> cascadeAnn = parentField.getAnnotationUsage( Cascade.class );
		final List<CascadeType> cascadeTypes = cascadeAnn.getList( "value" );
		assertThat( cascadeTypes ).isNotEmpty();
		assertThat( cascadeTypes ).containsOnly( CascadeType.ALL );
	}

	@SuppressWarnings("unused")
	@Entity(name="SimpleEntity")
	@Table(name="SimpleEntity")
	public static class SimpleEntity {
		@Id
		private Integer id;
		private String name;
		private SimpleEntity parent;
	}
}
