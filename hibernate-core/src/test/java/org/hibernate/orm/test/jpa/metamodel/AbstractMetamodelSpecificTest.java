/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.metamodel;

import jakarta.persistence.EntityManager;

import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.AfterAll;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractMetamodelSpecificTest extends EntityManagerFactoryBasedFunctionalTest {

	private EntityManager em;

	@AfterAll
	public final void closeEntityManager() {
		if ( em != null && em.isOpen() ) {
			em.close();
		}
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Address.class, Alias.class, Country.class, CreditCard.class, Customer.class,
				Entity1.class, Entity2.class, Entity3.class,
				Info.class, LineItem.class, Order.class, Phone.class, Product.class,
				ShelfLife.class, Spouse.class, Thing.class, ThingWithQuantity.class,
				VersionedEntity.class
		};
	}

	protected EntityManager getOrCreateEntityManager() {
		if ( em == null || !em.isOpen() ) {
			em = entityManagerFactory().createEntityManager();
		}
		return em;
	}
}
