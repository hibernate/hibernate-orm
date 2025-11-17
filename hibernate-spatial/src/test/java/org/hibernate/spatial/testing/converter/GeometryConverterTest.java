/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.converter;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spatial.GeolatteGeometryJavaType;
import org.hibernate.tool.schema.Action;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import org.geolatte.geom.Geometry;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;

/**
 * @author Steve Ebersole
 */
public class GeometryConverterTest extends BaseUnitTestCase {

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

				assertThat(
						typeConfiguration.getJavaTypeRegistry().getDescriptor( Geometry.class ),
						sameInstance( GeolatteGeometryJavaType.GEOMETRY_INSTANCE )
				);

				// todo (5.3) : what to assert wrt to SqlTypeDescriptor?  Anything?

				final EntityPersister entityPersister = sessionFactory.getMappingMetamodel().getEntityDescriptor( MyEntity.class );
				final ConvertedBasicTypeImpl geometryAttributeType = assertTyping(
						ConvertedBasicTypeImpl.class,
						entityPersister.getPropertyType( "geometry" )
				);

				final JpaAttributeConverter converter = assertTyping(
						JpaAttributeConverter.class,
						geometryAttributeType.getValueConverter()
				);

				assert GeometryConverter.class.equals( converter.getConverterBean().getBeanClass() );
			}
		}
	}

}
