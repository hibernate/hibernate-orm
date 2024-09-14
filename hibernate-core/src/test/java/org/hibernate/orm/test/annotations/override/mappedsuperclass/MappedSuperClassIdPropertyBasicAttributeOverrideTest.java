/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.override.mappedsuperclass;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Stanislav Gubanov
 */
@JiraKey(value = "HHH-11771")
public class MappedSuperClassIdPropertyBasicAttributeOverrideTest {

	@Test
	public void test() {
		try (StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry()) {

			MetadataSources metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClasses( MappedSuperClassWithUuidAsBasic.class );
			metadataSources.addAnnotatedClasses( SubclassWithUuidAsId.class );

			MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
			metadata.buildSessionFactory();
			fail( "Should throw exception!" );
		}
		catch (MappingException expected) {
			assertEquals(
					"You cannot override the [uid] non-identifier property from the [org.hibernate.orm.test.annotations.override.mappedsuperclass.MappedSuperClassWithUuidAsBasic] base class or @MappedSuperclass and make it an identifier in the [org.hibernate.orm.test.annotations.override.mappedsuperclass.SubclassWithUuidAsId] subclass",
					expected.getMessage()
			);
		}
	}

}
