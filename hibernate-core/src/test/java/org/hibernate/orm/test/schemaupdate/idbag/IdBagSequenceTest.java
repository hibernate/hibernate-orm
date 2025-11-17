/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.idbag;

import org.hamcrest.MatcherAssert;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;

import static org.hamcrest.core.Is.is;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-10373")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
@ServiceRegistry(settings = @Setting(name = HBM2DDL_AUTO, value = "none"))
@DomainModel(xmlMappings = "org/hibernate/orm/test/schemaupdate/idbag/Mappings.hbm.xml")
public class IdBagSequenceTest {

	@Test
	public void testIdBagSequenceGeneratorIsCreated(
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		final var scriptFile = new File( tmpDir, "update_script.sql" );

		final var metadata = modelScope.getDomainModel();
		metadata.orderColumns( false );
		metadata.validate();

		new SchemaUpdate()
				.setHaltOnError( true )
				.setOutputFile( scriptFile.getAbsolutePath() )
				.setDelimiter( ";" )
				.setFormat( true )
				.execute( EnumSet.of( TargetType.SCRIPT ), metadata );

		String fileContent = new String( Files.readAllBytes( scriptFile.toPath() ) );
		MatcherAssert.assertThat( fileContent.toLowerCase().contains( "create sequence seq_child_id" ), is( true ) );
	}

}
