/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query;

import java.util.List;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrIntTestEntity;
import org.hibernate.envers.test.support.domains.revisionentity.CustomRevEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class CustomRevisionEntityQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;
	private Integer id2;
	private Long timestamp;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrIntTestEntity.class, CustomRevEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() throws InterruptedException {
		List<Long> timestamps = inTransactionsWithTimeouts(
				100,

				// Revision 1
				entityManager -> {
					StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
					StrIntTestEntity site2 = new StrIntTestEntity( "b", 15 );

					entityManager.persist( site1 );
					entityManager.persist( site2 );

					id1 = site1.getId();
					id2 = site2.getId();
				},

				// Revision 2
				entityManager -> {
					final StrIntTestEntity site1 = entityManager.find( StrIntTestEntity.class, id1 );
					site1.setStr1( "c" );
				}
		);

		assertThat( timestamps, CollectionMatchers.hasSize( 3 ) );
		this.timestamp = timestamps.get( 1 );
	}

	@DynamicTest
	public void testRevisionsOfId1Query() {
		List<Object[]> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.add( AuditEntity.id().eq( id1 ) )
				.getResultList();

		assertThat( result.get( 0 )[0], equalTo( new StrIntTestEntity( "a", 10, id1 ) ) );
		assertThat( result.get( 0 )[1], instanceOf( CustomRevEntity.class ) );
		assertThat( ( (CustomRevEntity) result.get( 0 )[1] ).getCustomId(), equalTo( 1 ) );

		assertThat( result.get( 1 )[0], equalTo( new StrIntTestEntity( "c", 10, id1 ) ) );
		assertThat( result.get( 1 )[1], instanceOf( CustomRevEntity.class ) );
		assertThat( ( (CustomRevEntity) result.get( 1 )[1] ).getCustomId(), equalTo( 2 ) );
	}

	@DynamicTest
	public void testRevisionsOfId2Query() {
		List<Object[]> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.add( AuditEntity.id().eq( id2 ) )
				.getResultList();

		assertThat( result.get( 0 )[0], equalTo( new StrIntTestEntity( "b", 15, id2 ) ) );
		assertThat( result.get( 0 )[1], instanceOf( CustomRevEntity.class ) );
		assertThat( ( (CustomRevEntity) result.get( 0 )[1] ).getCustomId(), equalTo( 1 ) );
	}

	@DynamicTest
	public void testRevisionPropertyRestriction() {
		List<Object[]> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.add( AuditEntity.id().eq( id1 ) )
				.add( AuditEntity.revisionProperty( "customTimestamp" ).ge( timestamp ) )
				.getResultList();

		assertThat( result.get( 0 )[0], equalTo( new StrIntTestEntity( "c", 10, id1 ) ) );
		assertThat( result.get( 0 )[1], instanceOf( CustomRevEntity.class ) );
		assertThat( ( (CustomRevEntity) result.get( 0 )[1] ).getCustomId(), equalTo( 2 ) );
		assertThat( ( (CustomRevEntity) result.get( 0 )[1] ).getCustomTimestamp(), greaterThanOrEqualTo( timestamp ) );
	}
}