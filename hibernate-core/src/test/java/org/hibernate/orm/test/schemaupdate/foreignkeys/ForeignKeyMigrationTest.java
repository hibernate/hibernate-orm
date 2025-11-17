/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import java.util.EnumSet;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportCatalogCreation.class)
@ServiceRegistry
@DomainModel(annotatedClasses = {ForeignKeyMigrationTest.Box.class, ForeignKeyMigrationTest.Thing.class})
public class ForeignKeyMigrationTest {
	@Test
	@JiraKey( value = "HHH-9716" )
	public void testMigrationOfForeignKeys(DomainModelScope modelScope) {
		final var metadata = modelScope.getDomainModel();
		metadata.orderColumns( false );
		metadata.validate();

		// first create the schema...
		new SchemaExport().create( EnumSet.of( TargetType.DATABASE ), metadata );

		try {
			// try to update the just created schema
			new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), metadata );
		}
		finally {
			// clean up
			new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
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
