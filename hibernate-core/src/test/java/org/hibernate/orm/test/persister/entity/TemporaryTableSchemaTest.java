/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.persister.entity;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = {
		@Setting( name = AvailableSettings.DEFAULT_SCHEMA, value = TemporaryTableSchemaTest.CUSTOM_SCHEMA),
		@Setting( name = AvailableSettings.HBM2DDL_CREATE_SCHEMAS, value = "true")
})
@DomainModel(annotatedClasses = {
		TemporaryTableSchemaTest.Bar.class
})
@SessionFactory
public class TemporaryTableSchemaTest {

	static final String CUSTOM_SCHEMA = "CUSTOM_SCHEMA";

	@Test
	@JiraKey( value = "HHH-15517")
	public void test(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final SqmMultiTableInsertStrategy insertStrategy = sessionFactory
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( Bar.class )
				.getSqmMultiTableInsertStrategy();
		final TemporaryTable temporaryTable;
		if ( insertStrategy instanceof LocalTemporaryTableInsertStrategy ) {
			temporaryTable = ( (LocalTemporaryTableInsertStrategy) insertStrategy ).getTemporaryTable();
		}
		else if ( insertStrategy instanceof GlobalTemporaryTableInsertStrategy ) {
			temporaryTable = ( (GlobalTemporaryTableInsertStrategy) insertStrategy ).getTemporaryTable();
		}
		else if ( insertStrategy instanceof PersistentTableInsertStrategy ) {
			temporaryTable = ( (PersistentTableInsertStrategy) insertStrategy ).getTemporaryTable();
		}
		else {
			temporaryTable = null;
		}
		if ( temporaryTable == null ) {
			assertFalse( sessionFactory.getJdbcServices().getDialect().supportsTemporaryTables() );
			return;
		}
		assertTrue(
				temporaryTable.getQualifiedTableName().startsWith( "CUSTOM_SCHEMA" ),
				"Formula should not contain {} characters"
		);
	}


	@Entity(name = "BAR")
	@Table(name = "BAR")
	@SecondaryTable( name = "BAR2")
	public static class Bar {

		@Id
		@Column(name = "ID")
		public Integer id;

		@Column(name = "name")
		public String name;

		@Column(name = "name2", table = "BAR2")
		public String name2;
	}
}
