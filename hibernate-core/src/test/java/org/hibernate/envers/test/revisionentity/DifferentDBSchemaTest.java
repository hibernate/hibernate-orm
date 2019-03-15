/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.revisionentity;

import java.util.Map;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.metamodel.model.relational.spi.PhysicalTable;
import org.hibernate.metamodel.model.relational.spi.Table;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;
import org.hibernate.testing.orm.junit.RequiresDialect;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Tests simple auditing process (read and write operations) when <i>REVINFO</i> and audit tables
 * exist in a different database schema.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@RequiresDialect(H2Dialect.class)
public class DifferentDBSchemaTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private static final String SCHEMA_NAME = "ENVERS_AUDIT";
	private Integer steId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		// Set the default schema to be used.
		injectProperties( settings, Environment.getProperties() );
		settings.put( EnversSettings.DEFAULT_SCHEMA, SCHEMA_NAME );
	}

	@Override
	protected String secondSchema() {
		return SCHEMA_NAME;
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final StrTestEntity ste = new StrTestEntity( "x" );
					entityManager.persist( ste );
					steId = ste.getId();
				},

				// Revision 2
				entityManager -> {
					final StrTestEntity ste = entityManager.find( StrTestEntity.class, steId );
					ste.setStr( "y" );
				}
		);
	}

	@DynamicTest
	public void testRevinfoSchemaName() {
		final Table table = getMetamodel().getEntityDescriptor( SequenceIdRevisionEntity.class.getName() ).getPrimaryTable();
		assertThat( table, instanceOf( PhysicalTable.class ) );
		assertThat( ( (PhysicalTable) table ).getSchemaName().getText(), equalTo( SCHEMA_NAME ) );
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, steId ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		StrTestEntity ver1 = new StrTestEntity( steId, "x" );
		StrTestEntity ver2 = new StrTestEntity( steId, "y" );

		assertThat( getAuditReader().find( StrTestEntity.class, steId, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( StrTestEntity.class, steId, 2 ), equalTo( ver2 ) );
	}
}
