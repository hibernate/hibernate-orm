/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.Target;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * Regression test fr a bug in org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl
 *
 * @author Steve Ebersole
 *
 * @see
 */
@TestForIssue( jiraKey = "HHH-9745" )
public class SequenceReadingTest {

	@Test
	public void testSequenceReading() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, MyExtendedH2Dialect.class )
				.build();
		try {
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( MyEntity.class )
					.buildMetadata();
			metadata.validate();

			try {
				// try to update the schema
				new SchemaUpdate( metadata ).execute( Target.EXPORT );
			}
			finally {
				try {
					// clean up
					new SchemaExport( metadata ).drop( Target.EXPORT );
				}
				catch (Exception ignore) {
				}
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	/**
	 * An integral piece of the bug is Dialects which to not support sequence, so lets trick the test
	 */
	public static class MyExtendedH2Dialect extends H2Dialect {
		@Override
		public SequenceInformationExtractor getSequenceInformationExtractor() {
			return SequenceInformationExtractorNoOpImpl.INSTANCE;
		}

		@Override
		public String getQuerySequencesString() {
			return null;
		}
	}

	@Entity(name = "MyEntity")
	@Table(name = "my_entity")
	public static class MyEntity {
		@Id
		public Integer id;
	}

}
