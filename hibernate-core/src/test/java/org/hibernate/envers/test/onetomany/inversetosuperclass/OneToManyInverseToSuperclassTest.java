/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany.inversetosuperclass;

import java.util.ArrayList;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.inversetosuperclass.DetailSubclass;
import org.hibernate.envers.test.support.domains.onetomany.inversetosuperclass.DetailSubclass2;
import org.hibernate.envers.test.support.domains.onetomany.inversetosuperclass.Master;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Hern&aacute;n Chanfreau
 */
public class OneToManyInverseToSuperclassTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	private long m1_id;

	@Override
	protected String[] getMappings() {
		return new String[] { "oneToMany/inverseToSuperclass/mappings.hbm.xml" };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					Master m1 = new Master();
					DetailSubclass det1 = new DetailSubclass2();


					det1.setStr2( "detail 1" );

					m1.setStr( "master" );
					m1.setItems( new ArrayList<DetailSubclass>() );
					m1.getItems().add( det1 );
					det1.setParent( m1 );

					entityManager.persist( m1 );

					m1_id = m1.getId();
				},

				// Revision 2
				entityManager -> {
					DetailSubclass det2 = new DetailSubclass2();

					Master m1 = entityManager.find( Master.class, m1_id );

					det2.setStr2( "detail 2" );
					det2.setParent( m1 );
					m1.getItems().add( det2 );
				},

				// Revision 3
				entityManager -> {
					Master m1 = entityManager.find( Master.class, m1_id );
					m1.setStr( "new master" );

					DetailSubclass det1 = m1.getItems().get( 0 );
					det1.setStr2( "new detail" );
					DetailSubclass det3 = new DetailSubclass2();
					det3.setStr2( "detail 3" );
					det3.setParent( m1 );

					m1.getItems().get( 1 ).setParent( null );
					// m1.getItems().remove(1);
					m1.getItems().add( det3 );

					entityManager.persist( m1 );
				},

				// Revision 4
				entityManager -> {
					Master m1 = entityManager.find( Master.class, m1_id );

					DetailSubclass det1 = m1.getItems().get( 0 );
					det1.setParent( null );
					// m1.getItems().remove(det1);

					entityManager.persist( m1 );
				}
		);
	}

	@DynamicTest
	public void testHistoryExists() {
		assertThat( getAuditReader().find( Master.class, m1_id, 1 ), notNullValue() );
		assertThat( getAuditReader().find( Master.class, m1_id, 2 ), notNullValue() );
		assertThat( getAuditReader().find( Master.class, m1_id, 3 ), notNullValue() );
		assertThat( getAuditReader().find( Master.class, m1_id, 4 ), notNullValue() );
	}

}
