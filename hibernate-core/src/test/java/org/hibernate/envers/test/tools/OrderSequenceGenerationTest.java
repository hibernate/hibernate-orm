/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test verifies that the sequence is created properly both for database operations and export
 * scripts and that a basic entity using said sequence can be persisted and fetched via the
 * audit reader.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11131")
@RequiresDialectFeature( DialectChecks.SupportsSequences.class )
public class OrderSequenceGenerationTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	private File createSchema;
	private File dropSchema;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StrTestEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		settings.put( AvailableSettings.HBM2DDL_SCRIPTS_CREATE_TARGET, createSchema.toPath() );
		settings.put( AvailableSettings.HBM2DDL_SCRIPTS_DROP_TARGET, dropSchema.toPath() );
		settings.put( AvailableSettings.HBM2DDL_SCRIPTS_ACTION, "drop-and-create" );
		settings.put( AvailableSettings.HBM2DDL_DATABASE_ACTION, "drop-and-create" );
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
	}

	@DynamicBeforeAll
	public void createTemporarySchemaFiles() throws Exception {
		createSchema = File.createTempFile( "create_schema", ".sql" );
		dropSchema = File.createTempFile( "drop_schema", ".sql" );
	}

	@DynamicTest
	public void testCreateSequenceExportScripts() {
		// This is wrapped in a lambda to force the EntityManager to be created.
		inJPA(
				entityManager -> {
					final String[] createStrings = getDialect().getCreateSequenceStrings( "REVISION_GENERATOR", 1, 1 );

					final String content;
					try {
						content = new String( Files.readAllBytes( createSchema.toPath() ) ).toLowerCase();
					}
					catch ( IOException e ) {
						throw new RuntimeException( "Failed to read schema contents", e );
					}

					for ( int i = 0; i < createStrings.length; ++i ) {
						String testString = createStrings[i];
						if ( getDialect() instanceof Oracle8iDialect ) {
							testString = createStrings[ i ] + " ORDER";
						}

						assertThat( content, containsString( createStrings[i].toLowerCase() ) );
					}
				}
		);
	}

	@DynamicTest
	public void testBasicPersistAndAuditFetch() {
		inJPA(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();

						final StrTestEntity e = new StrTestEntity( "Acme" );
						entityManager.persist( e );
						entityManager.getTransaction().commit();

						entityManager.clear();

						final StrTestEntity rev1 = getAuditReader().find( StrTestEntity.class, e.getId(), 1 );
						assertThat( rev1, equalTo( e ) );
					}
					catch ( Exception e ) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
							throw e;
						}
					}
				}
		);
	}
}
