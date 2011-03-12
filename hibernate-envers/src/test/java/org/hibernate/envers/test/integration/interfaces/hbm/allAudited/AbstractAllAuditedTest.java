package org.hibernate.envers.test.integration.interfaces.hbm.allAudited;

import javax.persistence.EntityManager;

import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.envers.test.AbstractEntityTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.Arrays;

/**
 * @author Hernán Chanfreau
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractAllAuditedTest extends AbstractEntityTest {

	private long ai_id;
	private long nai_id;
	
    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        AuditedImplementor ai = new AuditedImplementor();
        ai.setData("La data");
        ai.setAuditedImplementorData("audited implementor data");
        
        NonAuditedImplementor nai = new NonAuditedImplementor();
        nai.setData("info");
        nai.setNonAuditedImplementorData("sttring");
        
        // Revision 1
        em.getTransaction().begin();

        em.persist(ai);
        
        em.persist(nai);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        ai = em.find(AuditedImplementor.class, ai.getId());
        nai = em.find(NonAuditedImplementor.class, nai.getId());

        ai.setData("La data 2");
        ai.setAuditedImplementorData("audited implementor data 2");

        nai.setData("info 2");
        nai.setNonAuditedImplementorData("sttring 2");

        em.getTransaction().commit();

        //

        ai_id = ai.getId();
        nai_id = nai.getId();
    }

    @Test
    public void testRevisions() {
        Assert.assertEquals(getAuditReader().getRevisions(AuditedImplementor.class, ai_id), Arrays.asList(1, 2));
    }

    @Test
    public void testRetrieveAudited() {
    	// levanto las versiones actuales
    	AuditedImplementor ai = getEntityManager().find(AuditedImplementor.class, ai_id);
    	assert ai != null;
    	SimpleInterface si = getEntityManager().find(SimpleInterface.class, ai_id);
    	assert si != null;

    	// levanto las de la revisión 1, ninguna debe ser null
    	AuditedImplementor ai_rev1 = getAuditReader().find(AuditedImplementor.class, ai_id, 1);
    	assert ai_rev1 != null;
    	SimpleInterface si_rev1 = getAuditReader().find(SimpleInterface.class, ai_id, 1);
    	assert si_rev1 != null;

        AuditedImplementor ai_rev2 = getAuditReader().find(AuditedImplementor.class, ai_id, 2);
    	assert ai_rev2 != null;
    	SimpleInterface si_rev2 = getAuditReader().find(SimpleInterface.class, ai_id, 2);
    	assert si_rev2 != null;
    		
    	// data de las actuales no debe ser null
    	Assert.assertEquals(ai.getData(), "La data 2");
    	Assert.assertEquals(si.getData(), "La data 2");
    	// la data de las revisiones no debe ser null
        Assert.assertEquals(ai_rev1.getData(), "La data");
        Assert.assertEquals(si_rev1.getData(), "La data");

        Assert.assertEquals(ai_rev2.getData(), "La data 2");
        Assert.assertEquals(si_rev2.getData(), "La data 2");
    }
    
    @Test
    public void testRetrieveNonAudited() {
    	// levanto las versiones actuales
    	NonAuditedImplementor nai = getEntityManager().find(NonAuditedImplementor.class, nai_id);
    	assert nai != null;
    	SimpleInterface si = getEntityManager().find(SimpleInterface.class, nai_id);
    	assert si != null;
    	
    	assert si.getData().equals(nai.getData());

    	try {
    		// levanto la revision  
    		getAuditReader().find(NonAuditedImplementor.class, nai_id, 1);
    		assert false;
    	} catch (Exception e) {
    		// no es auditable!!!
    		assert (e instanceof NotAuditedException);
		}
    	
    	// levanto la revision que no es auditable pero con la interfaz, el resultado debe ser null
   		SimpleInterface si_rev1 = getAuditReader().find(SimpleInterface.class, nai_id, 1);
   		assert si_rev1 == null;
   		
    }    
}
