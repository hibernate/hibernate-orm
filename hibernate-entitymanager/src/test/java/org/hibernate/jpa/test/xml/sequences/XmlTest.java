/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.xml.sequences;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;

/**
 * @author Emmanuel Bernard
 */
@RequiresDialectFeature( DialectChecks.SupportsSequences.class )
public class XmlTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testXmlMappingCorrectness() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.close();
	}

	@Override
	public String[] getEjb3DD() {
		return new String[] {
				"org/hibernate/jpa/test/xml/sequences/orm.xml",
				"org/hibernate/jpa/test/xml/sequences/orm2.xml",
		};
	}
}
