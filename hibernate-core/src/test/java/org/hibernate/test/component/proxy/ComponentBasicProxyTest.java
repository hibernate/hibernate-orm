/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.component.proxy;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import java.util.List;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Guillaume Smet
 * @author Oliver Libutzki
 */
public class ComponentBasicProxyTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				Person.class, Adult.class
		};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12786")
	public void testBasicProxyingWithProtectedMethodCalledInConstructor() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Adult adult = new Adult();
			adult.setName( "Arjun Kumar" );
			entityManager.persist( adult );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Adult> adultsCalledArjun = entityManager
					.createQuery( "SELECT a from Adult a WHERE a.name = :name", Adult.class )
					.setParameter( "name", "Arjun Kumar" ).getResultList();
			Adult adult = adultsCalledArjun.iterator().next();
			entityManager.remove( adult );
		} );
	}
}
