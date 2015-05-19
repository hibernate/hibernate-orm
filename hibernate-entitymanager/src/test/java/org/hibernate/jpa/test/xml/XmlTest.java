/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.xml;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.SharedCacheMode;

import junit.framework.Assert;
import org.junit.Test;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Emmanuel Bernard
 */
public class XmlTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testXmlMappingCorrectness() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.close();
	}

	@Test
	public void testXmlMappingWithCacheable() throws Exception{
		EntityManager em = getOrCreateEntityManager();
		SessionImplementor session = em.unwrap( SessionImplementor.class );
		EntityPersister entityPersister= session.getFactory().getEntityPersister( Lighter.class.getName() );
		Assert.assertTrue(entityPersister.hasCache());
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[0];
	}

	protected void addConfigOptions(Map options) {
		options.put(  AvailableSettings.SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE );
	}

	@Override
	public String[] getEjb3DD() {
		return new String[] {
				"org/hibernate/jpa/test/xml/orm.xml",
				"org/hibernate/jpa/test/xml/orm2.xml",
		};
	}
}
