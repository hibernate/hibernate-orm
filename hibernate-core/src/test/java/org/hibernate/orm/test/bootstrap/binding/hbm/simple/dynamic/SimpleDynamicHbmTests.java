/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.hbm.simple.dynamic;

import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
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
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = @Setting( name = AvailableSettings.HBM2DDL_AUTO, value = "create-drop" )
)
public class SimpleDynamicHbmTests {
	@Test
	public void testBinding(ServiceRegistryScope scope) {
		final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) new MetadataSources( scope.getRegistry() )
				.addResource( "org/hibernate/orm/test/bootstrap/binding/hbm/simple/dynamic/SimpleDynamicEntity.hbm.xml" )
				.buildMetadata()
				.buildSessionFactory();

		try {
			final EntityPersister entityDescriptor = sessionFactory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.findEntityDescriptor( "SimpleDynamicEntity" );

			final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
			assertThat( identifierMapping, instanceOf( BasicEntityIdentifierMapping.class ) );
			final BasicEntityIdentifierMapping bid = (BasicEntityIdentifierMapping) identifierMapping;
			assertThat( bid.getFetchableName(), is( "id" ) );
			assertThat( bid.getPartName(), is( EntityIdentifierMapping.ID_ROLE_NAME ) );

			assertThat( entityDescriptor.getNumberOfAttributeMappings(), is( 1 ) );
			assertThat( entityDescriptor.getNumberOfDeclaredAttributeMappings(), is( 1 ) );
			final AttributeMapping nameAttr = entityDescriptor.findAttributeMapping( "name" );
			assertThat( nameAttr, notNullValue() );

			sessionFactory.inTransaction(
					session -> {
						session.createQuery( "from SimpleDynamicEntity" ).list();
						session.createQuery( "select e from SimpleDynamicEntity e" ).list();
						session.createQuery( "select e from SimpleDynamicEntity e where e.name = 'abc'" ).list();
					}
			);
		}
		finally {
			sessionFactory.close();
		}
	}
}
