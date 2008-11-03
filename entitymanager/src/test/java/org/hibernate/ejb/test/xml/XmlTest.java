//$Id$
package org.hibernate.ejb.test.xml;

import javax.persistence.EntityManager;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class XmlTest extends TestCase {
	public void testXmlMappingCorrectness() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.close();
	}

	public Class[] getAnnotatedClasses() {
		return new Class[0];
	}

	@Override
	public String[] getEjb3DD() {
		return new String[] {
				"org/hibernate/ejb/test/xml/orm.xml",
				"org/hibernate/ejb/test/xml/orm2.xml",
		};
	}
}
