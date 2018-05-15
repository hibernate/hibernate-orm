/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.access;

import java.util.Arrays;

import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.access.MixedAccessTypeEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Chris Cranford
 */
public class MixedAccessTypeTest extends EnversSessionFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { MixedAccessTypeEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.id = doInHibernate( this::sessionFactory, session -> {
			final MixedAccessTypeEntity entity = new MixedAccessTypeEntity( "data" );
			session.save( entity );
			return entity.getIdSafe();
		} );

		doInHibernate( this::sessionFactory, session -> {
			final MixedAccessTypeEntity entity = session.find( MixedAccessTypeEntity.class, id );
			entity.setDataSafe( "data2" );
		} );
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( MixedAccessTypeEntity.class, id ), is( Arrays.asList( 1, 2 ) ) );
	}

	@DynamicTest
	public void testHistoryOfEntity1() {
		final MixedAccessTypeEntity ver1 = new MixedAccessTypeEntity( id, "data" );
		final MixedAccessTypeEntity ver2 = new MixedAccessTypeEntity( id, "data2" );

		final MixedAccessTypeEntity rev1 = getAuditReader().find( MixedAccessTypeEntity.class, id, 1 );
		final MixedAccessTypeEntity rev2 = getAuditReader().find( MixedAccessTypeEntity.class, id, 2 );

		assertThat( rev1.isDataSet(), is( true ) );
		assertThat( rev1, is( ver1 ) );

		assertThat( rev2.isDataSet(), is( true ) );
		assertThat( rev2, is( ver2 ) );
	}
}
