/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.hibernate.cfg.JdbcSettings.DIALECT;

/**
 * Regression test fr a bug in org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl
 *
 * @author Steve Ebersole
 *
 * @see
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( value = "HHH-9745" )
@RequiresDialect( H2Dialect.class )
@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = SequenceReadingTest.MyEntity.class)
public class SequenceReadingTest implements ServiceRegistryProducer {
	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		return builder.applySetting( DIALECT, MyExtendedH2Dialect.class ).build();
	}

	@Test
	public void testSequenceReading(DomainModelScope modelScope) {
		var model = modelScope.getDomainModel();
		model.orderColumns( false );
		model.validate();

		try {
			// try to update the schema
			new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), model );
		}
		finally {
			try {
				// clean up
				new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), model );
			}
			catch (Exception ignore) {
			}
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
