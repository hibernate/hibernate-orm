/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany.detached;

import java.util.ArrayList;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.onetomany.detached.ListRefCollEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicDetachedListTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer str1_id;
	private Integer str2_id;

	private Integer coll1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class, ListRefCollEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inJPA(
				entityManager -> {
					StrTestEntity str1 = new StrTestEntity( "str1" );
					StrTestEntity str2 = new StrTestEntity( "str2" );

					ListRefCollEntity coll1 = new ListRefCollEntity( 3, "coll1" );

					// Revision 1
					entityManager.getTransaction().begin();

					entityManager.persist( str1 );
					entityManager.persist( str2 );

					coll1.setCollection( new ArrayList<StrTestEntity>() );
					coll1.getCollection().add( str1 );
					entityManager.persist( coll1 );

					entityManager.getTransaction().commit();

					// Revision 2
					entityManager.getTransaction().begin();

					str2 = entityManager.find( StrTestEntity.class, str2.getId() );
					coll1 = entityManager.find( ListRefCollEntity.class, coll1.getId() );

					coll1.getCollection().add( str2 );

					entityManager.getTransaction().commit();

					// Revision 3
					entityManager.getTransaction().begin();

					str1 = entityManager.find( StrTestEntity.class, str1.getId() );
					coll1 = entityManager.find( ListRefCollEntity.class, coll1.getId() );

					coll1.getCollection().remove( str1 );

					entityManager.getTransaction().commit();

					// Revision 4
					entityManager.getTransaction().begin();

					coll1 = entityManager.find( ListRefCollEntity.class, coll1.getId() );

					coll1.getCollection().clear();

					entityManager.getTransaction().commit();

					str1_id = str1.getId();
					str2_id = str2.getId();
					coll1_id = coll1.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ListRefCollEntity.class, coll1_id ), contains( 1, 2, 3, 4 ) );

		assertThat( getAuditReader().getRevisions( StrTestEntity.class, str1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, str2_id ), contains( 1 ) );
	}

	@DynamicTest
	public void testHistoryOfColl1() {
		inTransaction(
				entityManager -> {
					final StrTestEntity str1 = entityManager.find( StrTestEntity.class, str1_id );
					final StrTestEntity str2 = entityManager.find( StrTestEntity.class, str2_id );

					final ListRefCollEntity rev1 = getAuditReader().find( ListRefCollEntity.class, coll1_id, 1 );
					final ListRefCollEntity rev2 = getAuditReader().find( ListRefCollEntity.class, coll1_id, 2 );
					final ListRefCollEntity rev3 = getAuditReader().find( ListRefCollEntity.class, coll1_id, 3 );
					final ListRefCollEntity rev4 = getAuditReader().find( ListRefCollEntity.class, coll1_id, 4 );

					assertThat( rev1.getCollection(), contains( str1 ) );
					assertThat( rev2.getCollection(), containsInAnyOrder( str1, str2 ) );
					assertThat( rev3.getCollection(), contains( str2 ) );
					assertThat( rev4.getCollection(), CollectionMatchers.isEmpty() );

					assertThat( rev1.getData(), equalTo( "coll1" ) );
					assertThat( rev2.getData(), equalTo( "coll1" ) );
					assertThat( rev3.getData(), equalTo( "coll1" ) );
					assertThat( rev4.getData(), equalTo( "coll1" ) );
				}
		);
	}
}