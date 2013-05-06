package org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited;

import javax.persistence.EntityManager;

import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Hern�n Chanfreau
 */
public abstract class AbstractPropertiesAuditedTest extends BaseEnversJPAFunctionalTestCase {
	private long ai_id;
	private long nai_id;

	private static int NUMERITO = 555;

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		AuditedImplementor ai = new AuditedImplementor();
		ai.setData( "La data" );
		ai.setAuditedImplementorData( "audited implementor data" );
		ai.setNumerito( NUMERITO );

		NonAuditedImplementor nai = new NonAuditedImplementor();
		nai.setData( "info" );
		nai.setNonAuditedImplementorData( "sttring" );
		nai.setNumerito( NUMERITO );

		// Revision 1
		em.getTransaction().begin();

		em.persist( ai );

		em.persist( nai );

		em.getTransaction().commit();

		// Revision 2

		// Revision 3

		ai_id = ai.getId();
		nai_id = nai.getId();
	}

	@Test
	public void testRetrieveAudited() {
		// levanto las versiones actuales
		AuditedImplementor ai = getEntityManager().find(
				AuditedImplementor.class, ai_id
		);
		assert ai != null;
		SimpleInterface si = getEntityManager().find(
				SimpleInterface.class,
				ai_id
		);
		assert si != null;

		// levanto las de la revisi�n 1, ninguna debe ser null
		AuditedImplementor ai_rev1 = getAuditReader().find(
				AuditedImplementor.class, ai_id, 1
		);
		assert ai_rev1 != null;
		SimpleInterface si_rev1 = getAuditReader().find(
				SimpleInterface.class,
				ai_id, 1
		);
		assert si_rev1 != null;

		// data de las actuales no debe ser null
		assert ai.getData() != null;
		assert si.getData() != null;
		// data de las revisiones No est� auditada
		assert ai_rev1.getData() == null;
		assert si_rev1.getData() == null;
		// numerito de las revisiones est� auditada, debe ser igual a NUMERITO
		assert ai_rev1.getNumerito() == NUMERITO;
		assert si_rev1.getNumerito() == NUMERITO;
	}

	@Test
	public void testRetrieveNonAudited() {
		// levanto las versiones actuales
		NonAuditedImplementor nai = getEntityManager().find(
				NonAuditedImplementor.class, nai_id
		);
		assert nai != null;
		SimpleInterface si = getEntityManager().find(
				SimpleInterface.class,
				nai_id
		);
		assert si != null;

		assert si.getData().equals( nai.getData() );

		try {
			// levanto la revision
			getAuditReader().find( NonAuditedImplementor.class, nai_id, 1 );
			assert false;
		}
		catch (Exception e) {
			// no es auditable!!!
			assert (e instanceof NotAuditedException);
		}

		// levanto la revision que no es auditable pero con la interfaz, el
		// resultado debe ser null
		SimpleInterface si_rev1 = getAuditReader().find(
				SimpleInterface.class,
				nai_id, 1
		);
		assert si_rev1 == null;
	}
}
