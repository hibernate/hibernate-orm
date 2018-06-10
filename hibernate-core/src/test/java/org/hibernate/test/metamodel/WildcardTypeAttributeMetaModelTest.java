/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.metamodel;

import javax.persistence.metamodel.EntityType;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.test.metamodel.wildcardmodel.AbstractEntity;
import org.hibernate.test.metamodel.wildcardmodel.AbstractOwner;
import org.hibernate.test.metamodel.wildcardmodel.EntityOne;
import org.hibernate.test.metamodel.wildcardmodel.OwnerOne;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;

public class WildcardTypeAttributeMetaModelTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AbstractEntity.class,
				AbstractOwner.class,
				EntityOne.class,
				OwnerOne.class
		};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9403")
	public void testWildcardGenericAttributeCanBeResolved() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			EntityType<AbstractOwner> entity = entityManager.getMetamodel().entity( AbstractOwner.class );
			assertNotNull( entity );
		} );
	}

}
