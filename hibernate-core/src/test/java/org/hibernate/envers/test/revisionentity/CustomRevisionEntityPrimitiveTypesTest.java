/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.revisionentity;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.revisionentity.CustomRevEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class CustomRevisionEntityPrimitiveTypesTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id;
	private long timestamp1;
	private long timestamp2;
	private long timestamp3;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class, CustomRevEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		final List<Long> timestamps = inTransactionsWithTimeouts(
				100,
				entityManager -> {
					final StrTestEntity entity = new StrTestEntity( "x" );
					entityManager.persist( entity );
					id = entity.getId();
				},

				entityManager -> {
					final StrTestEntity entity = entityManager.find( StrTestEntity.class, id );
					entity.setStr( "y" );
				}
		);

		assertThat( timestamps, CollectionMatchers.hasSize( 3 ) );

		this.timestamp1 = timestamps.get( 0 );
		this.timestamp2 = timestamps.get( 1 );
		this.timestamp3 = timestamps.get( 2 );
	}

	@DynamicTest(expected = RevisionDoesNotExistException.class)
	public void testTimestamps1() {
		getAuditReader().getRevisionNumberForDate( new Date( timestamp1 ) );
	}

	@DynamicTest
	public void testTimestamps() {
		assertThat( getAuditReader().getRevisionNumberForDate( new Date( timestamp2 ) ).intValue(), equalTo( 1 ) );
		assertThat( getAuditReader().getRevisionNumberForDate( new Date( timestamp3 ) ).intValue(), equalTo( 2 ) );
	}

	@DynamicTest
	public void testDatesForRevisions() {
		final AuditReader reader = getAuditReader();
		assertThat( reader.getRevisionNumberForDate( reader.getRevisionDate( 1 ) ).intValue(), equalTo( 1 ) );
		assertThat( reader.getRevisionNumberForDate( reader.getRevisionDate( 2 ) ).intValue(), equalTo( 2 ) );
	}

	@DynamicTest
	public void testRevisionsForDates() {
		final AuditReader reader = getAuditReader();

		final Number revisionNumberTimestamp2 = reader.getRevisionNumberForDate( new Date( timestamp2 ) );
		assertThat( reader.getRevisionDate( revisionNumberTimestamp2 ).getTime(), lessThanOrEqualTo( timestamp2 ) );
		assertThat( reader.getRevisionDate( revisionNumberTimestamp2.intValue() + 1).getTime(), greaterThan( timestamp2 ) );

		final Number revisionNumberTimestamp3 = reader.getRevisionNumberForDate( new Date( timestamp3 ) );
		assertThat( reader.getRevisionDate( revisionNumberTimestamp3 ).getTime(), lessThanOrEqualTo( timestamp3 ) );
	}

	@DynamicTest
	public void testFindRevision() {
		final AuditReader reader = getAuditReader();

		final long rev1Timestamp = reader.findRevision( CustomRevEntity.class, 1 ).getCustomTimestamp();
		assertThat( rev1Timestamp, greaterThan( timestamp1 ) );
		assertThat( rev1Timestamp, lessThanOrEqualTo( timestamp2 ) );

		final long rev2Timestamp = reader.findRevision( CustomRevEntity.class, 2 ).getCustomTimestamp();
		assertThat( rev2Timestamp, greaterThan( timestamp2 ) );
		assertThat( rev2Timestamp, lessThanOrEqualTo( timestamp3 ) );
	}

	@DynamicTest
	public void testFindRevisions() {
		final AuditReader reader = getAuditReader();

		Set<Number> revNumbers = new HashSet<Number>();
		revNumbers.add( 1 );
		revNumbers.add( 2 );

		Map<Number, CustomRevEntity> revisionMap = reader.findRevisions( CustomRevEntity.class, revNumbers );
		assertThat( revisionMap.entrySet(), CollectionMatchers.hasSize( 2 ) );

		final CustomRevEntity rev1 = reader.findRevision( CustomRevEntity.class, 1 );
		assertThat( revisionMap, hasEntry( 1, rev1 ) );

		final CustomRevEntity rev2 = reader.findRevision( CustomRevEntity.class, 2 );
		assertThat( revisionMap, hasEntry( 2, rev2 ) );
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, id ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		StrTestEntity ver1 = new StrTestEntity( id, "x" );
		StrTestEntity ver2 = new StrTestEntity( id, "y" );

		assertThat( getAuditReader().find( StrTestEntity.class, id, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( StrTestEntity.class, id, 2 ), equalTo( ver2 ) );
	}
}
