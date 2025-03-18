/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.tools;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.entities.StrTestEntity;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test verifies that the sequence is created properly both for database operations and export
 * scripts and that a basic entity using said sequence can be persisted and fetched via the
 * audit reader.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11131")
@RequiresDialectFeature( DialectChecks.SupportsSequences.class )
public class OrderSequenceGenerationTest extends BaseEnversJPAFunctionalTestCase {

	private File createSchema;
	private File dropSchema;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StrTestEntity.class };
	}

	@Override
	public void buildEntityManagerFactory() throws Exception {
		createSchema = File.createTempFile( "create_schema", ".sql" );
		dropSchema = File.createTempFile( "drop_schema", ".sql" );
		super.buildEntityManagerFactory();
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, createSchema.toPath() );
		options.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET, dropSchema.toPath() );
		options.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "drop-and-create" );
		options.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "create-drop" );
		options.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
	}

	@Test
	public void testCreateSequenceExportScripts() throws Exception {
		final String[] createStrings = getDialect().getSequenceSupport().getCreateSequenceStrings( "REVISION_GENERATOR", 1, 1 );
		final String content = new String( Files.readAllBytes( createSchema.toPath() ) ).toLowerCase();
		for ( int i = 0; i < createStrings.length; ++i ) {
			if ( getDialect() instanceof OracleDialect ) {
				assertTrue( content.contains( ( createStrings[ i ] + " NOCACHE ORDER" ).toLowerCase() ) );
			}
			else {
				assertTrue( content.contains( createStrings[ i ].toLowerCase() ) );
			}
		}
	}

	@Test
	public void testBasicPersistAndAuditFetch() throws Exception {
		EntityManager entityManager = getOrCreateEntityManager();
		try {
			StrTestEntity e = new StrTestEntity( "Acme" );
			entityManager.getTransaction().begin();
			entityManager.persist( e );
			entityManager.getTransaction().commit();
			entityManager.clear();

			StrTestEntity rev1 = getAuditReader().find ( StrTestEntity.class, e.getId(), 1 );
			assertEquals( e, rev1 );
		}
		catch ( Exception e ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}
}
