/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import java.util.Arrays;

import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.BasicAuditedEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Chris Cranford
 */
public class NullPropertiesTest extends EnversSessionFactoryBasedFunctionalTest {
	private Integer e1Id;
	private Integer e2Id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		e1Id = doInHibernate( this::sessionFactory, session -> {
			final BasicAuditedEntity e1 = new BasicAuditedEntity( "x", 1 );
			session.save( e1 );
			return e1.getId();
		} );

		e2Id = doInHibernate( this::sessionFactory, session -> {
			final BasicAuditedEntity e2 = new BasicAuditedEntity( null, 20 );
			session.save( e2 );
			return e2.getId();
		} );

		doInHibernate( this::sessionFactory, session -> {
			final BasicAuditedEntity entity = session.find( BasicAuditedEntity.class, e1Id );
			entity.setLong1( 1 );
			entity.setStr1( null );
			session.save( entity );
		} );

		doInHibernate( this::sessionFactory, session -> {
			final BasicAuditedEntity entity = session.find( BasicAuditedEntity.class, e2Id );
			entity.setLong1( 20 );
			entity.setStr1( "y2" );
			session.save( entity );
		} );
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( BasicAuditedEntity.class, e1Id ), is( Arrays.asList( 1, 3 ) ) );
		assertThat( getAuditReader().getRevisions( BasicAuditedEntity.class, e2Id ), is( Arrays.asList( 2, 4 ) ) );
	}

	@DynamicTest
	public void testHistoryOfEntity1() {
		BasicAuditedEntity ver1 = new BasicAuditedEntity( e1Id, "x", 1 );
		BasicAuditedEntity ver2 = new BasicAuditedEntity( e1Id, null, 1 );

		assertThat( getAuditReader().find( BasicAuditedEntity.class, e1Id, 1 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e1Id, 2 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e1Id, 3 ), is( ver2 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e1Id, 4 ), is( ver2 ) );
	}

	@DynamicTest
	public void testHistoryOfEntity2() {
		BasicAuditedEntity ver1 = new BasicAuditedEntity( e2Id, null, 20 );
		BasicAuditedEntity ver2 = new BasicAuditedEntity( e2Id, "y2", 20 );

		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 1 ), nullValue() );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 2 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 3 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 4 ), is( ver2 ) );
	}
}
