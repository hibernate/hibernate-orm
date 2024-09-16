/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.onetoone.basic;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Table;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class OneToOneSchemaTest {

	@Test
	public void testUniqueKeyNotGeneratedViaAnnotations() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( Parent.class )
					.addAnnotatedClass( Child.class )
					.buildMetadata();

			Table childTable = metadata.getDatabase().getDefaultNamespace().locateTable( Identifier.toIdentifier(
					"CHILD" ) );
			assertFalse( childTable.getUniqueKeys().values().iterator().hasNext(), "UniqueKey was generated when it should not" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
