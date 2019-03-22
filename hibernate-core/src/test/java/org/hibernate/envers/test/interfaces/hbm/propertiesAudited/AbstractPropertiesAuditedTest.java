/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.interfaces.hbm.propertiesAudited;

import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Hern&aacute;n Chanfreau
 */
public abstract class AbstractPropertiesAuditedTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private long ai_id;
	private long nai_id;

	private static int NUMERITO = 555;

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				entityManager -> {
					final AuditedImplementor ai = new AuditedImplementor();
					ai.setData( "La data" );
					ai.setAuditedImplementorData( "audited implementor data" );
					ai.setNumerito( NUMERITO );

					final NonAuditedImplementor nai = new NonAuditedImplementor();
					nai.setData( "info" );
					nai.setNonAuditedImplementorData( "sttring" );
					nai.setNumerito( NUMERITO );

					entityManager.persist( ai );
					entityManager.persist( nai );

					this.ai_id = ai.getId();
					this.nai_id = nai.getId();
				}
		);
	}

	@DynamicTest
	public void testRetrieveAudited() {
		// levanto las versiones actuales
		final AuditedImplementor ai = inJPA( em -> { return em.find( AuditedImplementor.class, ai_id ); } );
		assertThat( ai, notNullValue() );
		final SimpleInterface si = inJPA( em -> { return em.find( SimpleInterface.class, ai_id ); } );
		assertThat( si, notNullValue() );

		// levanto las de la revisi�n 1, ninguna debe ser null
		final AuditedImplementor ai_rev1 = getAuditReader().find( AuditedImplementor.class, ai_id, 1 );
		assertThat( ai_rev1, notNullValue() );
		final SimpleInterface si_rev1 = getAuditReader().find( SimpleInterface.class, ai_id, 1 );
		assertThat( si_rev1, notNullValue() );

		// data de las actuales no debe ser null
		assertThat( ai.getData(), notNullValue() );
		assertThat( si.getData(), notNullValue() );
		// data de las revisiones No est� auditada
		assertThat( ai_rev1.getData(), nullValue() );
		assertThat( si_rev1.getData(), nullValue() );
		// numerito de las revisiones est� auditada, debe ser igual a NUMERITO
		assertThat( ai_rev1.getNumerito(), equalTo( NUMERITO ) );
		assertThat( si_rev1.getNumerito(), equalTo( NUMERITO ) );
	}

	@DynamicTest
	public void testRetrieveNonAudited() {
		// levanto las versiones actuales
		final NonAuditedImplementor nai = inJPA( em -> { return em.find( NonAuditedImplementor.class, nai_id ); } );
		assertThat( nai, notNullValue() );
		final SimpleInterface si = inJPA( em -> { return em.find( SimpleInterface.class, nai_id ); } );
		assertThat( si, notNullValue() );

		assertThat( si.getData(), equalTo( nai.getData() ) );

		// levanto la revision que no es auditable pero con la interfaz, el resultado debe ser null
		final SimpleInterface si_rev1 = getAuditReader().find( SimpleInterface.class, nai_id, 1 );
		assertThat( si_rev1, nullValue() );
	}

	@DynamicTest(expected = NotAuditedException.class)
	public void testAuditReaderFindNotAuditedEntity() {
		getAuditReader().find( NonAuditedImplementor.class, nai_id, 1 );
	}
}
