/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.spatial.testing.converter;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spatial.GeolatteGeometryJavaTypeDescriptor;
import org.hibernate.spatial.dialect.h2geodb.GeoDBDialect;
import org.hibernate.tool.schema.Action;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;
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
		try ( final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, GeoDBDialect.class )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.build() ) {
			final MetadataSources metadataSources = new MetadataSources( ssr )
					.addAnnotatedClass( GeometryConverter.class )
					.addAnnotatedClass( MyEntity.class );
			final MetadataBuilderImplementor metadataBuilder = (MetadataBuilderImplementor) metadataSources.getMetadataBuilder();

			try ( final SessionFactoryImplementor sessionFactory =
						  (SessionFactoryImplementor) metadataBuilder.build().buildSessionFactory() ) {

				final TypeConfiguration typeConfiguration = sessionFactory.getMetamodel().getTypeConfiguration();

				assertThat(
						typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Geometry.class ),
						sameInstance( GeolatteGeometryJavaTypeDescriptor.INSTANCE )
				);

				// todo (5.3) : what to assert wrt to SqlTypeDescriptor?  Anything?

				final EntityPersister entityPersister = sessionFactory.getMetamodel().entityPersister( MyEntity.class );
				final AttributeConverterTypeAdapter geometryAttributeType = assertTyping(
						AttributeConverterTypeAdapter.class,
						entityPersister.getPropertyType( "geometry" )
				);

				final JpaAttributeConverter converter = assertTyping(
						JpaAttributeConverter.class,
						geometryAttributeType.getAttributeConverter()
				);

				assert GeometryConverter.class.equals( converter.getConverterBean().getBeanClass() );
			}
		}
	}

}
