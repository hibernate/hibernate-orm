/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.attr;

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
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

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

		final SourceModelBuildingContext sourceModelBuildingContext = createBuildingContext( managedResources, serviceRegistry );
		final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( SimpleEntity.class.getName() );

		final FieldDetails parentField = classDetails.findFieldByName( "parent" );
		final ManyToOne manyToOneAnn = parentField.getDirectAnnotationUsage( ManyToOne.class );
		assertThat( manyToOneAnn ).isNotNull();
		final JoinColumn joinColumnAnn = parentField.getAnnotationUsage( JoinColumn.class, sourceModelBuildingContext );
		assertThat( joinColumnAnn ).isNotNull();
		assertThat( joinColumnAnn.name() ).isEqualTo( "parent_fk" );

		final NotFound notFoundAnn = parentField.getDirectAnnotationUsage( NotFound.class );
		assertThat( notFoundAnn ).isNotNull();
		assertThat( notFoundAnn.action() ).isEqualTo( NotFoundAction.IGNORE );

		final OnDelete onDeleteAnn = parentField.getDirectAnnotationUsage( OnDelete.class );
		assertThat( onDeleteAnn ).isNotNull();
		assertThat( onDeleteAnn.action() ).isEqualTo( OnDeleteAction.CASCADE );

		final Fetch fetchAnn = parentField.getDirectAnnotationUsage( Fetch.class );
		assertThat( fetchAnn ).isNotNull();
		assertThat( fetchAnn.value() ).isEqualTo( FetchMode.SELECT );

		final OptimisticLock optLockAnn = parentField.getDirectAnnotationUsage( OptimisticLock.class );
		assertThat( optLockAnn ).isNotNull();
		assertThat( optLockAnn.excluded() ).isTrue();

		final Target targetAnn = parentField.getDirectAnnotationUsage( Target.class );
		assertThat( targetAnn ).isNotNull();
		assertThat( targetAnn.value() ).isEqualTo( "org.hibernate.orm.test.boot.models.xml.attr.ManyToOneTests$SimpleEntity" );

		final Cascade cascadeAnn = parentField.getDirectAnnotationUsage( Cascade.class );
		final CascadeType[] cascadeTypes = cascadeAnn.value();
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
