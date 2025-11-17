/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.hbm.allAudited;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Hernán Chanfreau
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractAllAuditedTest {
	private long ai_id;
	private long nai_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			AuditedImplementor ai = new AuditedImplementor();
			ai.setData( "La data" );
			ai.setAuditedImplementorData( "audited implementor data" );

			NonAuditedImplementor nai = new NonAuditedImplementor();
			nai.setData( "info" );
			nai.setNonAuditedImplementorData( "sttring" );

			em.persist( ai );
			em.persist( nai );

			ai_id = ai.getId();
			nai_id = nai.getId();
		} );

		// Revision 2
		scope.inTransaction( em -> {
			AuditedImplementor ai = em.find( AuditedImplementor.class, ai_id );
			NonAuditedImplementor nai = em.find( NonAuditedImplementor.class, nai_id );

			ai.setData( "La data 2" );
			ai.setAuditedImplementorData( "audited implementor data 2" );

			nai.setData( "info 2" );
			nai.setNonAuditedImplementorData( "sttring 2" );
		} );
	}

	@Test
	public void testRevisions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( AuditedImplementor.class, ai_id ) );
		} );
	}

	@Test
	public void testRetrieveAudited(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			// levanto las versiones actuales
			AuditedImplementor ai = em.find( AuditedImplementor.class, ai_id );
			assertNotNull( ai );
			SimpleInterface si = em.find( SimpleInterface.class, ai_id );
			assertNotNull( si );

			// levanto las de la revisión 1, ninguna debe ser null
			AuditedImplementor ai_rev1 = auditReader.find( AuditedImplementor.class, ai_id, 1 );
			assertNotNull( ai_rev1 );
			SimpleInterface si_rev1 = auditReader.find( SimpleInterface.class, ai_id, 1 );
			assertNotNull( si_rev1 );

			AuditedImplementor ai_rev2 = auditReader.find( AuditedImplementor.class, ai_id, 2 );
			assertNotNull( ai_rev2 );
			SimpleInterface si_rev2 = auditReader.find( SimpleInterface.class, ai_id, 2 );
			assertNotNull( si_rev2 );

			// data de las actuales no debe ser null
			assertEquals( "La data 2", ai.getData() );
			assertEquals( "La data 2", si.getData() );
			// la data de las revisiones no debe ser null
			assertEquals( "La data", ai_rev1.getData() );
			assertEquals( "La data", si_rev1.getData() );

			assertEquals( "La data 2", ai_rev2.getData() );
			assertEquals( "La data 2", si_rev2.getData() );
		} );
	}

	@Test
	public void testRetrieveNonAudited(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			// levanto las versiones actuales
			NonAuditedImplementor nai = em.find( NonAuditedImplementor.class, nai_id );
			assertNotNull( nai );
			SimpleInterface si = em.find( SimpleInterface.class, nai_id );
			assertNotNull( si );

			assertEquals( nai.getData(), si.getData() );

			try {
				// levanto la revision
				auditReader.find( NonAuditedImplementor.class, nai_id, 1 );
				throw new AssertionError( "Expected NotAuditedException" );
			}
			catch (Exception e) {
				// no es auditable!!!
				assertInstanceOf( NotAuditedException.class, e );
			}

			// levanto la revision que no es auditable pero con la interfaz, el resultado debe ser null
			SimpleInterface si_rev1 = auditReader.find( SimpleInterface.class, nai_id, 1 );
			assertNull( si_rev1 );
		} );
	}
}
