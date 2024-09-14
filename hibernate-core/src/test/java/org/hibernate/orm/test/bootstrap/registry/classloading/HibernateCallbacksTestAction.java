/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.registry.classloading;

import java.util.Collections;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;

/**
 * A test scenario to be used with {@link HibernateClassLoaderLeaksTest};
 * the crucial aspect is that we're triggering a lookup of a JPA callback
 * method.
 */
public class HibernateCallbacksTestAction extends HibernateLoadingTestAction {

	protected void actionOnHibernate(EntityManagerFactory emf) {
		try (final EntityManager entityManager = emf.createEntityManager() ) {
			Booking b = new Booking();
			b.id = Long.valueOf( 1l );
			entityManager.persist( b ); //to trigger the @PrePersist invocation
		}
	}

	protected List<String> getManagedClassNames() {
		return Collections.singletonList( Booking.class.getName() );
	}

	@Entity(name = "booking")
	private static class Booking {
		@Id Long id;
		@Transient String legacyIdentifier;

		@PrePersist
		public void computeLegacyIdentifier() {
			//Details are not important, just making something up.
			if ( legacyIdentifier == null && id != null ) {
				this.legacyIdentifier = id.toString();
			}
		}

	}

}
