/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.callbacks;

import java.util.Date;
import java.util.Map;
import javax.persistence.EntityManager;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.Cat;
import org.hibernate.jpa.test.Kitten;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Sanne Grinovero
 */
@SuppressWarnings("unchecked")
public class CallbacksDisabledTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	public void testCallbacksAreDisabled() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		Cat c = new Cat();
		c.setName( "Kitty" );
		c.setDateOfBirth( new Date( 90, 11, 15 ) );
		em.getTransaction().begin();
		em.persist( c );
		em.getTransaction().commit();
		em.clear();
		em.getTransaction().begin();
		c = em.find( Cat.class, c.getId() );
		assertTrue( c.getAge() == 0 ); // With listeners enabled this would be false. Proven by org.hibernate.jpa.test.callbacks.CallbacksTest.testCallbackMethod
		em.getTransaction().commit();
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Cat.class,
				Translation.class,
				Television.class,
				RemoteControl.class,
				Rythm.class,
				Plant.class,
				Kitten.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.JPA_CALLBACKS_ENABLED, "false" );
	}

}
