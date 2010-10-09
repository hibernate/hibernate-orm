//$Id: $
package org.hibernate.ejb.test.xml.sequences;

import javax.persistence.EntityManager;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class XmlAttributeOverrideTest extends TestCase {

	public void testAttributeOverriding() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

        Employee e = new Employee();
        e.setId(Long.valueOf(100));
        e.setName("Bubba");
        e.setHomeAddress(new Address("123 Main St", "New York", "NY", "11111"));
        e.setMailAddress(new Address("P.O. Box 123", "New York", "NY", "11111"));

        em.persist(e);

		em.flush();

		em.getTransaction().rollback();
		em.close();
	}

	public Class[] getAnnotatedClasses() {
		return new Class[0];
	}

	@Override
	public String[] getEjb3DD() {
		return new String[] {
				"org/hibernate/ejb/test/xml/sequences/orm3.xml"
		};
	}
}
