/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import java.util.Arrays;

import org.hibernate.Session;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.BasicAuditedEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Chris Cranford
 */
public class ManyOperationsInTransactionTest extends EnversSessionFactoryBasedFunctionalTest {
	private Integer e1Id;
	private Integer e2Id;
	private Integer e3Id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		Session session = openSession();
		try {
			// Revision 1
			session.getTransaction().begin();

			BasicAuditedEntity e1 = new BasicAuditedEntity( "x", 1 );
			BasicAuditedEntity e2 = new BasicAuditedEntity( "y", 20 );
			session.persist( e1 );
			session.persist( e2 );

			session.getTransaction().commit();

			// Revision 2
			session.getTransaction().begin();

			e1 = session.find( BasicAuditedEntity.class, e1.getId() );
			e2 = session.find( BasicAuditedEntity.class, e2.getId() );
			BasicAuditedEntity e3 = new BasicAuditedEntity( "z", 300 );
			e1.setStr1( "x2" );
			e2.setLong1( 21 );
			session.persist( e3 );

			session.getTransaction().commit();

			// Revision 3
			session.getTransaction().begin();
			e2 = session.find( BasicAuditedEntity.class, e2.getId() );
			e3 = session.find( BasicAuditedEntity.class, e3.getId() );
			e2.setStr1( "y3" );
			e2.setLong1( 22 );
			e3.setStr1( "z3" );
			session.getTransaction().commit();

			e1Id = e1.getId();
			e2Id = e2.getId();
			e3Id = e3.getId();
		}
		catch ( Exception e ) {
			if ( session.getTransaction().isActive() ) {
				session.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			if ( session != null && session.isOpen() ) {
				session.close();
			}
		}
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( BasicAuditedEntity.class, e1Id ), is( Arrays.asList( 1, 2 ) ) );
		assertThat( getAuditReader().getRevisions( BasicAuditedEntity.class, e2Id ), is( Arrays.asList( 1, 2, 3 ) ) );
		assertThat( getAuditReader().getRevisions( BasicAuditedEntity.class, e3Id ), is( Arrays.asList( 2, 3 ) ) );
	}

	@DynamicTest
	public void testHistoryOfEntity1() {
		BasicAuditedEntity ver1 = new BasicAuditedEntity( e1Id, "x", 1 );
		BasicAuditedEntity ver2 = new BasicAuditedEntity( e1Id, "x2", 1 );

		assertThat( getAuditReader().find( BasicAuditedEntity.class, e1Id, 1 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e1Id, 2 ), is( ver2 ) );
	}

	@DynamicTest
	public void testHistoryOfEntity2() {
		BasicAuditedEntity ver1 = new BasicAuditedEntity( e2Id, "y", 20 );
		BasicAuditedEntity ver2 = new BasicAuditedEntity( e2Id, "y", 21 );
		BasicAuditedEntity ver3 = new BasicAuditedEntity( e2Id, "y3", 22 );

		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 1 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 2 ), is( ver2 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e2Id, 3 ), is( ver3 ) );
	}

	@DynamicTest
	public void testHistoryOfEntity3() {
		BasicAuditedEntity ver1 = new BasicAuditedEntity( e3Id, "z", 300 );
		BasicAuditedEntity ver2 = new BasicAuditedEntity( e3Id, "z3", 300 );

		assertThat( getAuditReader().find( BasicAuditedEntity.class, e3Id, 1 ), nullValue() );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e3Id, 2 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicAuditedEntity.class, e3Id, 3 ), is( ver2 ) );
	}
}
