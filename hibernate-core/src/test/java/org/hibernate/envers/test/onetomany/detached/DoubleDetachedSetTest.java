/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany.detached;

import java.util.HashSet;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.onetomany.detached.DoubleSetRefCollEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DoubleDetachedSetTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer str1_id;
	private Integer str2_id;

	private Integer coll1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class, DoubleSetRefCollEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inJPA(
				entityManager -> {
					StrTestEntity str1 = new StrTestEntity( "str1" );
					StrTestEntity str2 = new StrTestEntity( "str2" );

					DoubleSetRefCollEntity coll1 = new DoubleSetRefCollEntity( 3, "coll1" );

					// Revision 1
					entityManager.getTransaction().begin();

					entityManager.persist( str1 );
					entityManager.persist( str2 );

					coll1.setCollection( new HashSet<>() );
					coll1.getCollection().add( str1 );
					entityManager.persist( coll1 );

					coll1.setCollection2( new HashSet<>() );
					coll1.getCollection2().add( str2 );
					entityManager.persist( coll1 );

					entityManager.getTransaction().commit();

					// Revision 2
					entityManager.getTransaction().begin();

					str2 = entityManager.find( StrTestEntity.class, str2.getId() );
					coll1 = entityManager.find( DoubleSetRefCollEntity.class, coll1.getId() );

					coll1.getCollection().add( str2 );

					entityManager.getTransaction().commit();

					// Revision 3
					entityManager.getTransaction().begin();

					str1 = entityManager.find( StrTestEntity.class, str1.getId() );
					coll1 = entityManager.find( DoubleSetRefCollEntity.class, coll1.getId() );

					coll1.getCollection().remove( str1 );
					coll1.getCollection2().add( str1 );

					entityManager.getTransaction().commit();

					str1_id = str1.getId();
					str2_id = str2.getId();
					coll1_id = coll1.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( DoubleSetRefCollEntity.class, coll1_id ), contains( 1, 2, 3 ) );

		assertThat( getAuditReader().getRevisions( StrTestEntity.class, str1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, str2_id ), contains( 1 ) );
	}

	@DynamicTest
	public void testHistoryOfColl1() {
		inTransaction(
				entityManager -> {
					final StrTestEntity str1 = entityManager.find( StrTestEntity.class, str1_id );
					final StrTestEntity str2 = entityManager.find( StrTestEntity.class, str2_id );

					final DoubleSetRefCollEntity rev1 = getAuditReader().find( DoubleSetRefCollEntity.class, coll1_id, 1 );
					final DoubleSetRefCollEntity rev2 = getAuditReader().find( DoubleSetRefCollEntity.class, coll1_id, 2 );
					final DoubleSetRefCollEntity rev3 = getAuditReader().find( DoubleSetRefCollEntity.class, coll1_id, 3 );

					assertThat( rev1.getCollection(), contains( str1 ) );
					assertThat( rev2.getCollection(), containsInAnyOrder( str1, str2 ) );
					assertThat( rev3.getCollection(), contains( str2 ) );

					assertThat( rev1.getCollection2(), contains( str2 ) );
					assertThat( rev2.getCollection2(), contains( str2 ) );
					assertThat( rev3.getCollection2(), containsInAnyOrder( str1, str2 ) );
				}
		);
	}
}