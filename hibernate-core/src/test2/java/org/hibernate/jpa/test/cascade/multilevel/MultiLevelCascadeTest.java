/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cascade.multilevel;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class MultiLevelCascadeTest extends BaseEntityManagerFunctionalTestCase {

	@TestForIssue( jiraKey = "HHH-5299" )
    @Test
	public void test() {
		final Top top = new Top();
		;
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {

			entityManager.persist( top );
			// Flush 1
			entityManager.flush();

			Middle middle = new Middle( 1l );
			top.addMiddle( middle );
			middle.setTop( top );
			Bottom bottom = new Bottom();
			middle.setBottom( bottom );
			bottom.setMiddle( middle );

			Middle middle2 = new Middle( 2l );
			top.addMiddle( middle2 );
			middle2.setTop( top );
			Bottom bottom2 = new Bottom();
			middle2.setBottom( bottom2 );
			bottom2.setMiddle( middle2 );
			// Flush 2
			entityManager.flush();
		} );

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {

			Top finded = entityManager.find( Top.class, top.getId() );

			assertEquals( 2, finded.getMiddles().size() );
			for ( Middle loadedMiddle : finded.getMiddles() ) {
				assertSame( finded, loadedMiddle.getTop() );
				assertNotNull( loadedMiddle.getBottom() );
			}
			entityManager.remove( finded );
		} );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Top.class, Middle.class, Bottom.class };
	}

}
