/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.util.dtd;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.MappingSettings;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class EntityResolverTest extends BaseUnitTestCase {
	@Test
	public void testEntityIncludeResolution() {
		// Parent.hbm.xml contains the following entity include:
		//		<!ENTITY child SYSTEM "classpath://org/hibernate/test/util/dtd/child.xml">
		// which we are expecting the Hibernate custom entity resolver to be able to resolve
		// locally via classpath lookup.
		buildMetadata( "org/hibernate/orm/test/util/dtd/Parent.hbm.xml" );
	}

	@Test
	public void testOrmXmlEntityIncludeResolution() {
		// Parent.orm.xml contains the following entity include:
		//		<!ENTITY child SYSTEM "classpath://org/hibernate/orm/test/util/dtd/child-entity.xml">
		final Metadata metadata = buildMetadata( "org/hibernate/orm/test/util/dtd/Parent.orm.xml" );
		assertNotNull( metadata.getEntityBinding( Child.class.getName() ) );
	}

	@Test
	public void testOrmXmlInternalEntityResolution() {
		// InternalEntity.orm.xml uses the following internal entity as the package name:
		//		<!ENTITY package "org.hibernate.orm.test.util.dtd">
		final Metadata metadata = buildMetadata( "org/hibernate/orm/test/util/dtd/InternalEntity.orm.xml" );
		assertNotNull( metadata.getEntityBinding( Parent.class.getName() ) );
	}

	private static Metadata buildMetadata(String resource) {
		final StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( MappingSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, "true" )
				.build();
		try {
			return new MetadataSources( serviceRegistry )
					.addResource( resource )
					.buildMetadata();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}
}
