/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.hbm.cid.nonaggregated.dynamic;

import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedIdentifierMappingImpl;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Note that this test uses a composite-id with key-many-to-one as part of a
 * dynamic model, which is the main construct needed by hibernate-envers
 *
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = @Setting( name = AvailableSettings.HBM2DDL_AUTO, value = "create-drop" )
)
public class DynamicCompositeIdManyToOneBindingTests {
	@Test
	public void testBinding(ServiceRegistryScope scope) {
		final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) new MetadataSources( scope.getRegistry() )
				.addResource( "org/hibernate/orm/test/bootstrap/binding/hbm/cid/nonaggregated/dynamic/DynamicCompositeIdManyToOne.hbm.xml" )
				.buildMetadata()
				.buildSessionFactory();

		try {
			final EntityPersister entityDescriptor = sessionFactory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.findEntityDescriptor( "DynamicCompositeIdManyToOne" );

			assertThat( entityDescriptor.getNumberOfAttributeMappings(), is( 1 ) );

			final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
			assertThat( identifierMapping, instanceOf( EmbeddedIdentifierMappingImpl.class ) );
			final EmbeddedIdentifierMappingImpl cid = (EmbeddedIdentifierMappingImpl) identifierMapping;
			assertThat( cid.getEmbeddableTypeDescriptor().getNumberOfAttributeMappings(), is( 2 ) );

			final AttributeMapping key1 = cid.getEmbeddableTypeDescriptor().findAttributeMapping( "key1" );
			assertThat( key1, notNullValue() );
			assertThat( key1, instanceOf( BasicAttributeMapping.class ) );

			final AttributeMapping key2 = cid.getEmbeddableTypeDescriptor().findAttributeMapping( "key2" );
			assertThat( key2, notNullValue() );
			assertThat( key2, instanceOf( ToOneAttributeMapping.class ) );

			final AttributeMapping attr1 = entityDescriptor.findAttributeMapping( "attr1" );
			assertThat( attr1, notNullValue() );
			assertThat( attr1, instanceOf( BasicAttributeMapping.class ) );
		}
		finally {
			sessionFactory.close();
		}
	}
}
