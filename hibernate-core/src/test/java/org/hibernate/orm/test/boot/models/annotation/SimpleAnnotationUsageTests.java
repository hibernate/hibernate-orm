/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.annotation;

import org.hibernate.boot.models.annotations.internal.EntityJpaAnnotation;
import org.hibernate.models.internal.OrmAnnotationDescriptor;
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
	void testSimpleUsage() {
		final ModelsContext context = SourceModelTestHelper.createBuildingContext( SimpleEntity.class );
		final AnnotationDescriptorRegistry descriptorRegistry = context.getAnnotationDescriptorRegistry();
		final AnnotationDescriptor<Entity> entityDescriptor = descriptorRegistry.getDescriptor( Entity.class );
		assertThat( entityDescriptor ).isInstanceOf( OrmAnnotationDescriptor.class );

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
