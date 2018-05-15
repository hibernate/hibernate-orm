/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import java.util.Arrays;

import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.BasicPartialAuditedEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Chris Cranford
 */
public class NotAuditedPropertyChangeTest extends EnversSessionFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicPartialAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.id = doInHibernate( this::sessionFactory, session -> {
			final BasicPartialAuditedEntity entity = new BasicPartialAuditedEntity( "x", "a" );
			session.persist( entity );
			return entity.getId();
		} );

		// Should not trigger a revision
		doInHibernate( this::sessionFactory, session -> {
			final BasicPartialAuditedEntity entity = session.find( BasicPartialAuditedEntity.class, this.id );
			entity.setStr1( "x" );
			entity.setStr2( "a" );
		} );

		doInHibernate( this::sessionFactory, session -> {
			final BasicPartialAuditedEntity entity = session.find( BasicPartialAuditedEntity.class, this.id );
			entity.setStr1( "y" );
			entity.setStr2( "b" );
		} );

		// Should not trigger a revision
		doInHibernate( this::sessionFactory, session -> {
			final BasicPartialAuditedEntity entity = session.find( BasicPartialAuditedEntity.class, this.id );
			entity.setStr1( "y" );
			entity.setStr2( "c" );
		} );
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat(
				getAuditReader().getRevisions( BasicPartialAuditedEntity.class, this.id ),
				is( Arrays.asList( 1, 2 ) )
		);
	}

	@DynamicTest
	public void testHistoryOfEntity1() {
		BasicPartialAuditedEntity ver1 = new BasicPartialAuditedEntity( id, "x", null );
		BasicPartialAuditedEntity ver2 = new BasicPartialAuditedEntity( id, "y", null );

		assertThat( getAuditReader().find( BasicPartialAuditedEntity.class, id, 1 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicPartialAuditedEntity.class, id, 2 ), is( ver2 ) );
	}
}
