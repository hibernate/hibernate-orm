/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.inheritance.hhh_x;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.Target;

import org.junit.Test;

/**
 * @author Andrea Boriero
 */
public class InheritanceSchemaUpdateTest {

	@Test
	public void testBidirectionalOneToManyReferencingRootEntity() throws Exception {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( Step.class )
				.addAnnotatedClass( GroupStep.class )
				.buildMetadata();
		metadata.validate();

		try {
			SchemaUpdate su = new SchemaUpdate( ssr, metadata );
			su.execute( Target.EXPORT );
		}
		finally {
			SchemaExport schemaExport = new SchemaExport( ssr, metadata );
			schemaExport.drop( Target.EXPORT );
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
