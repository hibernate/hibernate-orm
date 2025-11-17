/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.tools;

import java.io.File;
import java.nio.file.Files;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.StrTestEntity;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test verifies that the sequence is created properly both for database operations and export
 * scripts and that a basic entity using said sequence can be persisted and fetched via the
 * audit reader.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11131")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrderSequenceGenerationTest {

	private SessionFactoryImplementor sf;

	private File createSchema;
	private File dropSchema;
	private Integer entityId;

	@BeforeAll
	public void initData() throws Exception {
		createSchema = File.createTempFile( "create_schema", ".sql" );
		createSchema.deleteOnExit();

		dropSchema = File.createTempFile( "drop_schema", ".sql" );
		dropSchema.deleteOnExit();

		final var cfg = new Configuration();
		cfg.addAnnotatedClass( StrTestEntity.class );
		final var ssrb = cfg.getStandardServiceRegistryBuilder();
		ServiceRegistryUtil.applySettings( ssrb );
		// Configure settings for DDL script generation
		ssrb.applySetting( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, createSchema.toPath() );
		ssrb.applySetting( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET, dropSchema.toPath() );
		ssrb.applySetting( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "drop-and-create" );
		ssrb.applySetting( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "create-drop" );
		ssrb.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		// Standard envers test settings
		ssrb.applySetting( EnversSettings.USE_REVISION_ENTITY_WITH_NATIVE_ID, "false" );
		ssrb.applySetting( EnversSettings.REVISION_SEQUENCE_NOCACHE, "true" );

		sf = cfg.buildSessionFactory().unwrap( SessionFactoryImplementor.class );

		sf.inTransaction( em -> {
			StrTestEntity e = new StrTestEntity( "Acme" );
			em.persist( e );
			entityId = e.getId();
		} );
	}

	@AfterAll
	public void cleanUp() {
		if ( sf != null ) {
			sf.close();
		}
	}

	@Test
	public void testCreateSequenceExportScripts() throws Exception {
		final var dialect = sf.getJdbcServices().getDialect();
		final String[] createStrings = dialect
				.getSequenceSupport()
				.getCreateSequenceStrings( "REVISION_GENERATOR", 1, 1 );
		final String content = new String( Files.readAllBytes( createSchema.toPath() ) ).toLowerCase();
		for ( final var createString : createStrings ) {
			if ( dialect instanceof OracleDialect ) {
				assertTrue( content.contains( ( createString + " NOCACHE ORDER" ).toLowerCase() ) );
			}
			else {
				assertTrue( content.contains( createString.toLowerCase() ) );
			}
		}
	}

	@Test
	public void testBasicPersistAndAuditFetch() {
		sf.inSession( session -> {
			StrTestEntity rev1 = AuditReaderFactory.get( session ).find( StrTestEntity.class, entityId, 1 );
			assertEquals( new StrTestEntity( "Acme", entityId ), rev1 );
		} );
	}
}
