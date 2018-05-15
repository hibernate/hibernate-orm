/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import java.util.Arrays;
import java.util.Collections;

import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.BasicAuditedEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Chris Cranford
 */
public class SingleOperationInTransactionTest extends EnversSessionFactoryBasedFunctionalTest {
	private Integer e1Id;
	private Integer e2Id;
	private Integer e3Id;
	private Integer invalidId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		this.e1Id = doInHibernate( this::sessionFactory, session -> {
			BasicAuditedEntity entity = new BasicAuditedEntity( "x", 1 );
			session.persist( entity );
			return entity.getId();
		} );

		this.e2Id = doInHibernate( this::sessionFactory, session -> {
			BasicAuditedEntity entity = new BasicAuditedEntity( "y", 20 );
			session.persist( entity );
			return entity.getId();
		} );

		this.e3Id = doInHibernate( this::sessionFactory, session -> {
			BasicAuditedEntity entity = new BasicAuditedEntity( "z", 30 );
			session.persist( entity );
			return entity.getId();
		} );

		doInHibernate( this::sessionFactory, session -> {
			final BasicAuditedEntity entity = session.find( BasicAuditedEntity.class, e1Id );
			entity.setStr1( "x2" );
			entity.setLong1( 2 );
			session.update( entity );
		} );

		doInHibernate( this::sessionFactory, session -> {
			final BasicAuditedEntity entity = session.find( BasicAuditedEntity.class, e2Id );
			entity.setStr1( "y2" );
			entity.setLong1( 20 );
			session.update( entity );
		} );

		doInHibernate( this::sessionFactory, session -> {
			final BasicAuditedEntity entity = session.find( BasicAuditedEntity.class, e1Id );
			entity.setStr1( "x3" );
			entity.setLong1( 3 );
			session.update( entity );
		} );

		// shouldn't trigger a revision
		doInHibernate( this::sessionFactory, session -> {
			final BasicAuditedEntity entity = session.find( BasicAuditedEntity.class, e1Id );
			entity.setStr1( "x3" );
			entity.setLong1( 3 );
			session.update( entity );
		} );

		doInHibernate( this::sessionFactory, session -> {
			final BasicAuditedEntity entity = session.find( BasicAuditedEntity.class, e2Id );
			entity.setStr1( "y3" );
			entity.setLong1( 21 );
			session.update( entity );
		} );

		this.invalidId = e1Id + e2Id + e3Id;
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( BasicAuditedEntity.class, e1Id ), is( Arrays.asList( 1, 4, 6 ) ) );
		assertThat( getAuditReader().getRevisions( BasicAuditedEntity.class, e2Id ), is( Arrays.asList( 2, 5, 7 ) ) );
		assertThat( getAuditReader().getRevisions( BasicAuditedEntity.class, e3Id ), is( Collections.singletonList( 3 ) ) );
	}

	@DynamicTest
	public void testHistoryOfEntity1() {
		BasicAuditedEntity ver1 = new BasicAuditedEntity( e1Id, "x", 1 );
		BasicAuditedEntity ver2 = new BasicAuditedEntity( e1Id, "x2", 2 );
		BasicAuditedEntity ver3 = new BasicAuditedEntity( e1Id, "x3", 3 );

		assertThat( getAuditReader().find( BasicAuditedEntity.class, e1Id, 1 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e1Id, 2 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e1Id, 3 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e1Id, 4 ), is( ver2 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e1Id, 5 ), is( ver2 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e1Id, 6 ), is( ver3 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e1Id, 7 ), is( ver3 ) );
	}

	@DynamicTest
	public void testHistoryOfEntity2() {
		BasicAuditedEntity ver1 = new BasicAuditedEntity( e2Id, "y", 20 );
		BasicAuditedEntity ver2 = new BasicAuditedEntity( e2Id, "y2", 20 );
		BasicAuditedEntity ver3 = new BasicAuditedEntity( e2Id, "y3", 21 );

		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 1 ), nullValue() );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 2 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 3 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 4 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 5 ), is( ver2 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 6 ), is( ver2 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 7 ), is( ver3 ) );
	}

	@DynamicTest
	public void testHistoryOfEntity3() {
		BasicAuditedEntity ver1 = new BasicAuditedEntity( e3Id, "z", 30 );

		assertThat( getAuditReader().find( BasicAuditedEntity.class, e3Id, 1 ), nullValue() );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e3Id, 2 ), nullValue() );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e3Id, 3 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e3Id, 4 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e3Id, 5 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e3Id, 6 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e3Id, 7 ), is( ver1 ) );
	}

	@DynamicTest
	public void testRevisionDates() {
		for ( int i = 1; i < 7; ++i ) {
			assertThat(
					getAuditReader().getRevisionDate( i ).getTime(),
					lessThanOrEqualTo( getAuditReader().getRevisionDate( i ).getTime() )
			);
		}
	}

	@DynamicTest
	public void testHistoryOfNotExistEntity() {
		assertThat( getAuditReader().find( BasicAuditedEntity.class, invalidId, 1 ), nullValue() );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, invalidId, 7 ), nullValue() );
	}

	@DynamicTest
	public void testRevisionsOfNotExistEntity() {
		assertThat( getAuditReader().getRevisions( BasicAuditedEntity.class, invalidId ).size(), is( 0 ) );
	}

	@DynamicTest(expected = RevisionDoesNotExistException.class)
	public void testDoesNotExistRevision() {
		getAuditReader().getRevisionDate( 8 );
	}

	@DynamicTest(expected = IllegalArgumentException.class)
	public void testIllegalRevision() {
		getAuditReader().getRevisionDate( 0 );
	}
}
