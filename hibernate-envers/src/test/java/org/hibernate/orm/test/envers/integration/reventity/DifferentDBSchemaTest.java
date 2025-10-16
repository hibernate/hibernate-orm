/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.mapping.Table;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.AfterClassTemplate;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests simple auditing process (read and write operations) when <i>REVINFO</i> and audit tables
 * exist in a different database schema.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@RequiresDialect(H2Dialect.class)
@EnversTest
@DomainModel(annotatedClasses = {StrTestEntity.class})
@ServiceRegistry(settings = @Setting(name = EnversSettings.DEFAULT_SCHEMA, value = DifferentDBSchemaTest.SCHEMA_NAME))
@SessionFactory(exportSchema = false)
public class DifferentDBSchemaTest {
	static final String SCHEMA_NAME = "ENVERS_AUDIT";
	private Integer steId = null;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		// Create the schema used for audit tables as well
		scope.getSessionFactory().getSchemaManager().create( true );

		// Revision 1
		scope.inTransaction( em -> {
			StrTestEntity ste = new StrTestEntity( "x" );
			em.persist( ste );
			steId = ste.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			StrTestEntity ste = em.find( StrTestEntity.class, steId );
			ste.setStr( "y" );
		} );
	}

	@AfterClassTemplate
	public void cleanUp(SessionFactoryScope scope) {
		// Drop the schema used for audit tables as well
		scope.getSessionFactory().getSchemaManager().dropMappedObjects( true );
	}

	@Test
	public void testRevinfoSchemaName(DomainModelScope scope) {
		Table revisionTable = scope.getDomainModel()
				.getEntityBinding( "org.hibernate.envers.enhanced.SequenceIdRevisionEntity" ).getTable();
		assertEquals( SCHEMA_NAME, revisionTable.getSchema() );
	}

	@Test
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( em -> {
			assertEquals( Arrays.asList( 1, 2 ),
					AuditReaderFactory.get( em ).getRevisions( StrTestEntity.class, steId ) );
		} );
	}

	@Test
	public void testHistoryOfId1(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			StrTestEntity ver1 = new StrTestEntity( "x", steId );
			StrTestEntity ver2 = new StrTestEntity( "y", steId );

			assertEquals( ver1, auditReader.find( StrTestEntity.class, steId, 1 ) );
			assertEquals( ver2, auditReader.find( StrTestEntity.class, steId, 2 ) );
		} );
	}
}
