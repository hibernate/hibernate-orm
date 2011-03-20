/**
 * 
 */
package org.hibernate.envers.test.integration.onetomany.bidirectional;

import java.util.List;

import javax.persistence.EntityManager;

import org.dom4j.dom.DOMElement;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.integration.onetomany.inverseToSuperclass.Master;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class OneToManyBidirectionalTest extends AbstractEntityTest {
	
	private Long domainId;

	/**
	 * @see org.hibernate.envers.test.AbstractEntityTest#configure(org.hibernate.ejb.Ejb3Configuration)
	 */
	@Override
	public void configure(Ejb3Configuration cfg) {
		cfg.addAnnotatedClass(Domain.class);
		cfg.addAnnotatedClass(Faq.class);
	}

	@BeforeClass(dependsOnMethods = "init")
	public void initData() {
		EntityManager em = getEntityManager();
		
		Domain domain = new Domain();
		Faq faq = new Faq();
		
		// Revision 1
		em.getTransaction().begin();
		
		faq.setDomain(domain);
		faq.setFaqText("Revision 1");
		
		em.persist(domain);
		em.persist(faq);
		em.getTransaction().commit();
		
		domainId = domain.getId();
		
		Faq faq2 = new Faq();
		
		// Revision 2
		em.getTransaction().begin();
		
		
		faq2.setDomain(domain);
		faq.setFaqText("Revision 2");
		faq2.setFaqText("Revision 2");
		
		em.persist(faq2);
		em.persist(faq);
		em.getTransaction().commit();
	}
	
	@Test
	public void testLoadCollection() {
		Domain domain = getAuditReader().find(Domain.class, domainId, 2);
		
		List<Faq> faqs = domain.getFaqs();
		faqs.size();
	}
}
