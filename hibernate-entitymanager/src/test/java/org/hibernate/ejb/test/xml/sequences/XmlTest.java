//$Id: $
package org.hibernate.ejb.test.xml.sequences;

import javax.persistence.EntityManager;

import org.hibernate.dialect.Dialect;
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
	public boolean appliesTo(Dialect dialect) {
		return dialect.supportsSequences();
	}
	
	@Override
	public String[] getEjb3DD() {
		return new String[] {
				"org/hibernate/ejb/test/xml/sequences/orm.xml",
				"org/hibernate/ejb/test/xml/sequences/orm2.xml",
		};
	}
}
