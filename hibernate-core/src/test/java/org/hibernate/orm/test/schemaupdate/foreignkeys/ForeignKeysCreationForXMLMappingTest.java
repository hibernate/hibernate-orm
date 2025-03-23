/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BaseUnitTest
@RequiresDialect(H2Dialect.class)
public class ForeignKeysCreationForXMLMappingTest {

	@Test
	public void testForeignKeyCreation() throws Exception {
		File output = File.createTempFile( "person_fk_script", ".sql" );
		output.deleteOnExit();
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addResource( "org/hibernate/orm/test/schemaupdate/foreignkeys/customer.orm.xml" )
					.buildMetadata();
			metadata.validate();

			new SchemaExport().setOutputFile( output.getAbsolutePath() ).createOnly(
					EnumSet.of( TargetType.SCRIPT ),
					metadata
			);

			final List<String> sqlLines = Files.readAllLines( output.toPath(), Charset.defaultCharset() );
			assertThat( sqlLines.size() ).isNotEqualTo( 0 );
			assertTrue( checkFKCreation( sqlLines ), "Foreign keys have not been created" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	private boolean checkFKCreation(List<String> sqlLines) {
		for ( String sql : sqlLines ) {
			if ( sql.contains( "foreign key" ) ) {
				return true;
			}
		}
		return false;
	}

}
