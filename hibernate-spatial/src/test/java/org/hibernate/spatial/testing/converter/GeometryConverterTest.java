/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.converter;

import org.geolatte.geom.Geometry;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spatial.GeolatteGeometryJavaType;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.tool.schema.Action;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class GeometryConverterTest {

	@Test
	public void testConverterUsage() {
		try (final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, H2Dialect.class )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.build()) {
			final MetadataSources metadataSources = new MetadataSources( ssr )
					.addAnnotatedClass( GeometryConverter.class )
					.addAnnotatedClass( MyEntity.class );
			final MetadataBuilderImplementor metadataBuilder = (MetadataBuilderImplementor) metadataSources.getMetadataBuilder();

			try (final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) metadataBuilder.build()
					.buildSessionFactory()) {

				final TypeConfiguration typeConfiguration = sessionFactory.getMappingMetamodel().getTypeConfiguration();

				assertThat( typeConfiguration.getJavaTypeRegistry().resolveDescriptor( Geometry.class ) )
						.isSameAs( GeolatteGeometryJavaType.GEOMETRY_INSTANCE );

				// todo (5.3) : what to assert wrt to SqlTypeDescriptor?  Anything?

				final EntityPersister entityPersister = sessionFactory.getMappingMetamodel()
						.getEntityDescriptor( MyEntity.class );
				Type geometryAttributeType = entityPersister.getPropertyType( "geometry" );
				assertThat( geometryAttributeType ).isInstanceOf( ConvertedBasicTypeImpl.class );
				BasicValueConverter valueConverter = ((ConvertedBasicTypeImpl) geometryAttributeType).getValueConverter();
				assertThat( valueConverter ).isInstanceOf( JpaAttributeConverter.class );

				assertThat( ((JpaAttributeConverter) valueConverter).getConverterBean().getBeanClass() ).isEqualTo(
						GeometryConverter.class );
			}
		}
	}

}
