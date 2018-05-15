/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.BasicAuditedEntity;
import org.hibernate.envers.test.support.domains.basic.BasicNonAuditedEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Chris Cranford
 */
public class NotAuditedTest extends EnversSessionFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicAuditedEntity.class, BasicNonAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.id = doInHibernate( this::sessionFactory, session -> {
			BasicNonAuditedEntity entity = new BasicNonAuditedEntity( "x", "y" );
			session.persist( entity );
			return entity.getId();
		} );

		doInHibernate( this::sessionFactory, session -> {
			final BasicNonAuditedEntity entity = session.find( BasicNonAuditedEntity.class, id );
			entity.setStr1( "a" );
			entity.setStr2( "b" );
		} );
	}

	@DynamicTest(expected = NotAuditedException.class)
	public void testRevisionCounts() {
		getAuditReader().getRevisions( BasicNonAuditedEntity.class, this.id );
	}

	@DynamicTest(expected = NotAuditedException.class)
	public void testRevisionHistory() {
		getAuditReader().find( BasicNonAuditedEntity.class, this.id, 1 );
	}
}
