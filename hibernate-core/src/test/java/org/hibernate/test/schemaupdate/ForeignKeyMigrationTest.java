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
package org.hibernate.test.schemaupdate;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.Target;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class ForeignKeyMigrationTest extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-9716" )
//	@FailureExpected( jiraKey = "HHH-9716" )
	public void testMigrationOfForeignKeys() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		try {
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( Box.class )
					.addAnnotatedClass( Thing.class )
					.buildMetadata();
			metadata.validate();

			// first create the schema...
			new SchemaExport( metadata ).create( Target.EXPORT );

			// try to update the just created schema
			new SchemaUpdate( metadata ).execute( Target.EXPORT );

			// clean up
			new SchemaExport( metadata ).drop( Target.EXPORT );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "Box")
	@Table(name = "Box", schema = "PUBLIC", catalog = "DB1")
	public static class Box {
		@Id
		public Integer id;
		@ManyToOne
		@JoinColumn
		public Thing thing1;
	}

	@Entity(name = "Thing")
	@Table(name = "Thing", schema = "PUBLIC", catalog = "DB1")
	public static class Thing {
		@Id
		public Integer id;
	}
}
