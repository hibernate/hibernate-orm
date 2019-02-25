/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import java.util.List;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class SimpleTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StrTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		id = inTransaction(
				entityManager -> {
					final StrTestEntity entity = new StrTestEntity();
					entity.setStr( "simple" );
					entityManager.persist( entity );
					return entity.getId();
				}
		);

		inTransaction(
				entityManager -> {
					final StrTestEntity entity = entityManager.find( StrTestEntity.class, id );
					entity.setStr( "simple2" );
					entityManager.merge( entity );
				}
		);

		inTransaction(
				entityManager -> {
					final StrTestEntity entity = entityManager.find( StrTestEntity.class, id );
					entityManager.remove( entity );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, id ), hasItems( 1, 2, 3 ) );
	}

	@DynamicTest
	public void testRevisionsofEntityQuery() {
		List<?> results = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrTestEntity.class, false, true )
				.getResultList();
		assertThat( results, CollectionMatchers.hasSize( 3 ) );

		Object[] rev1 = (Object[]) results.get( 0 );
		assertThat( rev1[0], instanceOf( StrTestEntity.class ) );
		assertThat( rev1[1], instanceOf( SequenceIdRevisionEntity.class ) );
		assertThat( ( (SequenceIdRevisionEntity) rev1[1] ).getId(), is( 1 ) );
		assertThat( rev1[2], instanceOf( RevisionType.class ) );
		assertThat( rev1[2], is( RevisionType.ADD ) );

		Object[] rev2 = (Object[]) results.get( 1 );
		assertThat( rev2[0], instanceOf( StrTestEntity.class ) );
		assertThat( rev2[1], instanceOf( SequenceIdRevisionEntity.class ) );
		assertThat( ( (SequenceIdRevisionEntity) rev2[1] ).getId(), is( 2 ) );
		assertThat( rev2[2], instanceOf( RevisionType.class ) );
		assertThat( rev2[2], is( RevisionType.MOD ) );

		Object[] rev3 = (Object[]) results.get( 2 );
		assertThat( rev3[0], instanceOf( StrTestEntity.class ) );
		assertThat( rev3[1], instanceOf( SequenceIdRevisionEntity.class ) );
		assertThat( ( (SequenceIdRevisionEntity) rev3[1] ).getId(), is( 3 ) );
		assertThat( rev3[2], instanceOf( RevisionType.class ) );
		assertThat( rev3[2], is( RevisionType.DEL ) );
	}

	@DynamicTest
	public void testRevisionHistory() {
		final StrTestEntity rev1 = getAuditReader().find( StrTestEntity.class, id, 1 );
		assertThat( rev1, notNullValue() );
		assertThat( rev1.getStr(), is( "simple" ) );

		final StrTestEntity rev2 = getAuditReader().find( StrTestEntity.class, id, 2 );
		assertThat( rev2, notNullValue() );
		assertThat( rev2.getStr(), is( "simple2" ) );

		final StrTestEntity rev3 = getAuditReader().find( StrTestEntity.class, id, 3 );
		assertThat( rev3, nullValue() );
	}
}
