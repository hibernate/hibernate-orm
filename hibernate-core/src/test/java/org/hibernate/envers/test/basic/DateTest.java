/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import java.util.Date;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.DateTestEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DateTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { DateTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		DateTestEntity dte = new DateTestEntity( new Date( 12345000 ) );
		em.persist( dte );
		id1 = dte.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		dte = em.find( DateTestEntity.class, id1 );
		dte.setDateValue( new Date( 45678000 ) );
		em.getTransaction().commit();
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( DateTestEntity.class, id1 ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		DateTestEntity ver1 = new DateTestEntity( id1, new Date( 12345000 ) );
		DateTestEntity ver2 = new DateTestEntity( id1, new Date( 45678000 ) );

		assertThat( getAuditReader().find( DateTestEntity.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( DateTestEntity.class, id1, 2 ), equalTo( ver2 ) );
	}
}