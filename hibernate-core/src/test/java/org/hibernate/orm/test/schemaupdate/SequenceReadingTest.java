/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.util.EnumSet;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

/**
 * Regression test fr a bug in org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl
 *
 * @author Steve Ebersole
 *
 * @see
 */
@JiraKey( value = "HHH-9745" )
@RequiresDialect( H2Dialect.class )
public class SequenceReadingTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testSequenceReading() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, MyExtendedH2Dialect.class )
				.build();
		try {
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( MyEntity.class )
					.buildMetadata();
			metadata.orderColumns( false );
			metadata.validate();

			try {
				// try to update the schema
				new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), metadata );
			}
			finally {
				try {
					// clean up
					new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
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
