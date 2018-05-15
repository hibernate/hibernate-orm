/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import java.util.Arrays;

import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.BasicPartialNotAuditedEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Chris Cranford
 */
public class NotAuditedExplicitPropertyTest extends EnversSessionFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicPartialNotAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.id = doInHibernate( this::sessionFactory, session -> {
			final BasicPartialNotAuditedEntity entity = new BasicPartialNotAuditedEntity( "a1", "b1" );
			session.save( entity );
			return entity.getId();
		} );

		doInHibernate( this::sessionFactory, session -> {
			final BasicPartialNotAuditedEntity entity = session.find( BasicPartialNotAuditedEntity.class, this.id );
			entity.setData1( "a2" );
			entity.setData2( "b2" );
		} );
	}

	@DynamicTest
	public void testRevisionCount() {
		assertThat(
				getAuditReader().getRevisions( BasicPartialNotAuditedEntity.class, id ),
				is( Arrays.asList( 1, 2 ) )
		);
	}

	@DynamicTest
	public void testHistoryOfEntity1() {
		BasicPartialNotAuditedEntity ver1 = new BasicPartialNotAuditedEntity( id, "a1", null );
		BasicPartialNotAuditedEntity ver2 = new BasicPartialNotAuditedEntity( id, "a2", null );

		assertThat( getAuditReader().find( BasicPartialNotAuditedEntity.class, id, 1 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicPartialNotAuditedEntity.class, id, 2 ), is( ver2 ) );
	}
}
