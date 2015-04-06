/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.foreignkeys.disabled;

import java.util.Map;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Table.ForeignKeyKey;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Steve Ebersole
 */
public class DisabledForeignKeyTest extends BaseUnitTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-9704" )
	public void basicTests() {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		StandardServiceRegistry standardRegistry = registryBuilder.build();
		try {
			final MetadataSources sources = new MetadataSources( standardRegistry );

			sources.addAnnotatedClass( ManyToManyOwner.class );
			sources.addAnnotatedClass( ManyToManyTarget.class );

			final MetadataImplementor metadata = (MetadataImplementor) sources.buildMetadata();
			metadata.validate();

			new SchemaExport( metadata ).execute( true, false, false, true );

			int fkCount = 0;
			for ( Table table : metadata.collectTableMappings() ) {
				for ( Map.Entry<ForeignKeyKey, ForeignKey> entry : table.getForeignKeys().entrySet() ) {
					assertFalse(
							"Creation for ForeignKey [" + entry.getKey() + "] was not disabled",
							entry.getValue().isCreationEnabled()
					);
					fkCount++;
				}
			}

			// ultimately I want to actually create the ForeignKet reference, but simply disable its creation
			// via ForeignKet#disableCreation()
			assertEquals( "Was expecting 4 FKs", 0, fkCount );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( standardRegistry );
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9704" )
	public void expandedTests() {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		StandardServiceRegistry standardRegistry = registryBuilder.build();
		try {
			final MetadataSources sources = new MetadataSources( standardRegistry );

			sources.addAnnotatedClass( ManyToManyOwner.class );
			sources.addAnnotatedClass( ManyToManyTarget.class );

			final MetadataImplementor metadata = (MetadataImplementor) sources.buildMetadata();
			metadata.validate();

			// export the schema
			new SchemaExport( metadata ).execute( false, true, false, false );

			try {
				// update the schema
				new SchemaUpdate( metadata ).execute( false, true );
			}
			finally {
				// drop the schema
				new SchemaExport( metadata ).execute( false, true, false, false );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( standardRegistry );
		}
	}

}
