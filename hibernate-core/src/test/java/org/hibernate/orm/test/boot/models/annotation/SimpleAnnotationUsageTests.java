package org.hibernate.orm.test.boot.models.annotation;

import java.util.Map;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.EntityJpaAnnotation;
import org.hibernate.models.CompleteAnnotationDescriptor;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.orm.test.boot.models.SourceModelTestHelper;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class SimpleAnnotationUsageTests {
	@Test
	void testAnnotationCreation() {
		final ModelsContext context = SourceModelTestHelper.createBuildingContext( SimpleEntity.class );
		final EntityJpaAnnotation dynamic = JpaAnnotations.ENTITY.createUsage( context );
		assertThat( dynamic.name() ).isEmpty();
		dynamic.name( "DynamicEntity" );
		assertThat( dynamic.name() ).isEqualTo( "DynamicEntity" );

		final EntityJpaAnnotation reflected = JpaAnnotations.ENTITY.createUsage(
				SimpleEntity.class.getAnnotation( Entity.class ), context );
		assertThat( reflected.name() ).isEqualTo( "SimpleEntity" );
		reflected.name( "ReflectedEntity" );
		assertThat( reflected.name() ).isEqualTo( "ReflectedEntity" );

		final Entity mapped = JpaAnnotations.ENTITY.createUsage( Map.of( "name", "MappedEntity" ), context );
		assertThat( mapped ).isInstanceOf( EntityJpaAnnotation.class );
		assertThat( mapped.name() ).isEqualTo( "MappedEntity" );
		( (EntityJpaAnnotation) mapped ).name( "UpdatedMappedEntity" );
		assertThat( mapped.name() ).isEqualTo( "UpdatedMappedEntity" );
	}

	@Test
	void testRepeatableContainerRegistration() {
		final ModelsContext context = SourceModelTestHelper.createBuildingContext( SimpleEntity.class );
		final AnnotationDescriptorRegistry registry = context.getAnnotationDescriptorRegistry();
		assertThat( registry.getDescriptor( jakarta.persistence.AttributeOverride.class ) )
				.isSameAs( JpaAnnotations.ATTRIBUTE_OVERRIDE );
		assertThat( JpaAnnotations.ATTRIBUTE_OVERRIDE.getRepeatableContainer() )
				.isSameAs( registry.getDescriptor( jakarta.persistence.AttributeOverrides.class ) );
		assertThat( registry.getContainedRepeatableDescriptor( jakarta.persistence.AttributeOverrides.class ) )
				.isSameAs( JpaAnnotations.ATTRIBUTE_OVERRIDE );
	}

	@Test
	void testSimpleUsage() {
		final ModelsContext context = SourceModelTestHelper.createBuildingContext( SimpleEntity.class );
		final AnnotationDescriptorRegistry descriptorRegistry = context.getAnnotationDescriptorRegistry();
		final AnnotationDescriptor<Entity> entityDescriptor = descriptorRegistry.getDescriptor( Entity.class );
		assertThat( entityDescriptor ).isInstanceOf( CompleteAnnotationDescriptor.class );

		final ClassDetailsRegistry classDetailsRegistry = context.getClassDetailsRegistry();
		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( SimpleEntity.class.getName() );
		final Entity entityAnnotation = classDetails.getDirectAnnotationUsage( Entity.class );
		assertThat( entityAnnotation ).isInstanceOf( EntityJpaAnnotation.class );
		assertThat( entityAnnotation.name() ).isEqualTo( SimpleEntity.class.getSimpleName() );
		( (EntityJpaAnnotation) entityAnnotation ).name( "SomethingNew" );
		assertThat( entityAnnotation.name() ).isEqualTo( "SomethingNew" );
	}

	@Entity(name="SimpleEntity")
	@Table(name="SimpleEntity")
	public static class SimpleEntity {
		@Id
		private Integer id;
		private String name;
	}
}
