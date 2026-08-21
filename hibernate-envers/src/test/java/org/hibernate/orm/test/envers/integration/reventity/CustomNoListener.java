/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.reventity.CustomDataRevEntity;

import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.envers.junit.EnversTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class, CustomDataRevEntity.class})
public class CustomNoListener {
	private Integer id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			StrTestEntity te = new StrTestEntity( "x" );
			em.persist( te );
			id = te.getId();

			// Setting the data on the revision entity
			CustomDataRevEntity custom = AuditReaderFactory.get( em ).getCurrentRevision( CustomDataRevEntity.class, false );
			custom.setData( "data1" );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			StrTestEntity te = em.find( StrTestEntity.class, id );
			te.setStr( "y" );

			// Setting the data on the revision entity
			CustomDataRevEntity custom = AuditReaderFactory.get( em ).getCurrentRevision( CustomDataRevEntity.class, false );
			custom.setData( "data2" );
		} );

		// Revision 3 - no changes, but rev entity should be persisted
		scope.inTransaction( em -> {
			// Setting the data on the revision entity
			CustomDataRevEntity custom = AuditReaderFactory.get( em ).getCurrentRevision( CustomDataRevEntity.class, true );
			custom.setData( "data3" );
		} );

		// No changes, rev entity won't be persisted
		scope.inTransaction( em -> {
			// Setting the data on the revision entity
			CustomDataRevEntity custom = AuditReaderFactory.get( em ).getCurrentRevision( CustomDataRevEntity.class, false );
			custom.setData( "data4" );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			StrTestEntity te = em.find( StrTestEntity.class, id );
			te.setStr( "z" );

			// Setting the data on the revision entity
			CustomDataRevEntity custom = AuditReaderFactory.get( em ).getCurrentRevision( CustomDataRevEntity.class, false );
			custom.setData( "data5" );

			custom = AuditReaderFactory.get( em ).getCurrentRevision( CustomDataRevEntity.class, false );
			custom.setData( "data5bis" );
		} );
	}

	@Test
	public void testFindRevision(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( "data1", auditReader.findRevision( CustomDataRevEntity.class, 1 ).getData() );
			assertEquals( "data2", auditReader.findRevision( CustomDataRevEntity.class, 2 ).getData() );
			assertEquals( "data3", auditReader.findRevision( CustomDataRevEntity.class, 3 ).getData() );
			assertEquals( "data5bis", auditReader.findRevision( CustomDataRevEntity.class, 4 ).getData() );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2, 4 ), AuditReaderFactory.get( em ).getRevisions( StrTestEntity.class, id ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			StrTestEntity ver1 = new StrTestEntity( "x", id );
			StrTestEntity ver2 = new StrTestEntity( "y", id );
			StrTestEntity ver3 = new StrTestEntity( "z", id );

			assertEquals( ver1, auditReader.find( StrTestEntity.class, id, 1 ) );
			assertEquals( ver2, auditReader.find( StrTestEntity.class, id, 2 ) );
			assertEquals( ver2, auditReader.find( StrTestEntity.class, id, 3 ) );
			assertEquals( ver3, auditReader.find( StrTestEntity.class, id, 4 ) );
		} );
	}
}
