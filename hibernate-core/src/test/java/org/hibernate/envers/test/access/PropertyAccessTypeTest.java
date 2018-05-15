/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.access;

import java.util.Arrays;

import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.access.PropertyAccessTypeEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Chris Cranford
 */
public class PropertyAccessTypeTest extends EnversSessionFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { PropertyAccessTypeEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.id = doInHibernate( this::sessionFactory, session -> {
			final PropertyAccessTypeEntity entity = new PropertyAccessTypeEntity( "data" );
			session.save( entity );
			return entity.getId();
		} );

		doInHibernate( this::sessionFactory, session -> {
			final PropertyAccessTypeEntity entity = session.find( PropertyAccessTypeEntity.class, id );
			entity.setDataSafe( "data2" );
		} );
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( PropertyAccessTypeEntity.class, id ), is( Arrays.asList( 1, 2 ) ) );
	}

	@DynamicTest
	public void testHistoryOfEntity1() {
		final PropertyAccessTypeEntity ver1 = new PropertyAccessTypeEntity( id, "data" );
		final PropertyAccessTypeEntity ver2 = new PropertyAccessTypeEntity( id, "data2" );

		final PropertyAccessTypeEntity rev1 = getAuditReader().find( PropertyAccessTypeEntity.class, id, 1 );
		final PropertyAccessTypeEntity rev2 = getAuditReader().find( PropertyAccessTypeEntity.class, id, 2 );

		assertThat( rev1.isIdSet(), is( true ) );
		assertThat( rev2.isIdSet(), is( true ) );

		assertThat( rev1.isDataSet(), is( true ) );
		assertThat( rev2.isDataSet(), is( true ) );

		assertThat( rev1, is( ver1 ) );
		assertThat( rev2, is( ver2 ) );
	}
}
