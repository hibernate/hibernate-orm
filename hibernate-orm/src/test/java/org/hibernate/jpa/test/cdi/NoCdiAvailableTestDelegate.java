/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cdi;

import java.util.Collections;

import javax.persistence.EntityManagerFactory;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.test.PersistenceUnitInfoAdapter;

/**
 * @author Steve Ebersole
 */
public class NoCdiAvailableTestDelegate {
	public static EntityManagerFactory passingNoBeanManager() {
		return new HibernatePersistenceProvider().createContainerEntityManagerFactory(
				new PersistenceUnitInfoAdapter(),
				Collections.emptyMap()
		);
	}

	public static void passingBeanManager() {
		new HibernatePersistenceProvider().createContainerEntityManagerFactory(
				new PersistenceUnitInfoAdapter(),
				Collections.singletonMap( AvailableSettings.CDI_BEAN_MANAGER, new Object() )
		);
	}
}
