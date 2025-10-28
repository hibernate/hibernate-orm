/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.hbm.propertiesAudited;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Hernán Chanfreau
 */
public abstract class AbstractPropertiesAuditedTest {
	private long ai_id;
	private long nai_id;

	private static int NUMERITO = 555;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			AuditedImplementor ai = new AuditedImplementor();
			ai.setData( "La data" );
			ai.setAuditedImplementorData( "audited implementor data" );
			ai.setNumerito( NUMERITO );

			NonAuditedImplementor nai = new NonAuditedImplementor();
			nai.setData( "info" );
			nai.setNonAuditedImplementorData( "sttring" );
			nai.setNumerito( NUMERITO );

			em.persist( ai );
			em.persist( nai );

			ai_id = ai.getId();
			nai_id = nai.getId();
		} );
	}

	@Test
	public void testRetrieveAudited(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// levanto las versiones actuales
			AuditedImplementor ai = em.find( AuditedImplementor.class, ai_id );
			assertNotNull( ai );
			SimpleInterface si = em.find( SimpleInterface.class, ai_id );
			assertNotNull( si );

			final var auditReader = AuditReaderFactory.get( em );

			// levanto las de la revisión 1, ninguna debe ser null
			AuditedImplementor ai_rev1 = auditReader.find( AuditedImplementor.class, ai_id, 1 );
			assertNotNull( ai_rev1 );
			SimpleInterface si_rev1 = auditReader.find( SimpleInterface.class, ai_id, 1 );
			assertNotNull( si_rev1 );

			// data de las actuales no debe ser null
			assertNotNull( ai.getData() );
			assertNotNull( si.getData() );
			// data de las revisiones No está auditada
			assertNull( ai_rev1.getData() );
			assertNull( si_rev1.getData() );
			// numerito de las revisiones está auditada, debe ser igual a NUMERITO
			assertEquals( NUMERITO, ai_rev1.getNumerito() );
			assertEquals( NUMERITO, si_rev1.getNumerito() );
		} );
	}

	@Test
	public void testRetrieveNonAudited(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// levanto las versiones actuales
			NonAuditedImplementor nai = em.find( NonAuditedImplementor.class, nai_id );
			assertNotNull( nai );
			SimpleInterface si = em.find( SimpleInterface.class, nai_id );
			assertNotNull( si );

			assertEquals( nai.getData(), si.getData() );

			final var auditReader = AuditReaderFactory.get( em );

			// levanto la revision
			assertThrows( NotAuditedException.class, () -> {
				auditReader.find( NonAuditedImplementor.class, nai_id, 1 );
			} );

			// levanto la revision que no es auditable pero con la interfaz, el
			// resultado debe ser null
			SimpleInterface si_rev1 = auditReader.find( SimpleInterface.class, nai_id, 1 );
			assertNull( si_rev1 );
		} );
	}
}
