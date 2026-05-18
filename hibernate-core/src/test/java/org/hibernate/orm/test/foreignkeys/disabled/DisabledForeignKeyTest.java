/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.foreignkeys.disabled;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class DisabledForeignKeyTest {

	@Test
	@JiraKey(value = "HHH-9704")
	public void basicTests() {
		final var standardRegistry = ServiceRegistryUtil.serviceRegistry();
		try {
			final var sources = new MetadataSources( standardRegistry );

			sources.addAnnotatedClass( ManyToManyOwner.class );
			sources.addAnnotatedClass( ManyToManyTarget.class );

			final var metadata = (MetadataImplementor) sources.buildMetadata();
			metadata.orderColumns( false );
			metadata.validate();

			new SchemaExport().execute(
					EnumSet.of( TargetType.STDOUT ),
					SchemaExport.Action.CREATE,
					metadata
			);

			int fkCount = 0;
			for ( var table : metadata.collectTableMappings() ) {
				for ( var foreignKey : table.getForeignKeys() ) {
					assertThat( foreignKey.isCreationEnabled() )
							.describedAs( "Creation for ForeignKey [" + foreignKey.getName() + "] was not disabled" )
							.isFalse();
					fkCount++;
				}
			}

			// ultimately I want to actually create the ForeignKet reference, but simply disable its creation
			// via ForeignKet#disableCreation()
			assertThat( fkCount )
					.describedAs( "Was expecting 4 FKs" )
					.isEqualTo( 0 );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( standardRegistry );
		}
	}

	@Test
	@JiraKey(value = "HHH-9704")
	public void expandedTests() {
		final var standardRegistry = ServiceRegistryUtil.serviceRegistry();
		try {
			final var sources = new MetadataSources( standardRegistry );

			sources.addAnnotatedClass( ManyToManyOwner.class );
			sources.addAnnotatedClass( ManyToManyTarget.class );

			final var metadata = (MetadataImplementor) sources.buildMetadata();
			metadata.orderColumns( false );
			metadata.validate();

			// export the schema
			new SchemaExport().execute(
					EnumSet.of( TargetType.DATABASE ),
					SchemaExport.Action.BOTH,
					metadata
			);

			try {
				// update the schema
				new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), metadata );
			}
			finally {
				// drop the schema
				new SchemaExport().execute(
						EnumSet.of( TargetType.DATABASE ),
						SchemaExport.Action.DROP,
						metadata
				);
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( standardRegistry );
		}
	}

}
