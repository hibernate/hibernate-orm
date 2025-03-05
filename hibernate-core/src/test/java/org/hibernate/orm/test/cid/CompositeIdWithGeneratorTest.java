/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cid;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests the use of composite-id with a generator.
 * Test this behavior in all the various entity states (transient, managed, detached)
 * and the different state transitions.
 * <p>
 * For HHH-2060.
 *
 * @author Jacob Robertson
 */
@JiraKey("HHH-2060")
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/cid/PurchaseRecord.hbm.xml"
		}
)
@SessionFactory
public class CompositeIdWithGeneratorTest {
	private DateFormat df = SimpleDateFormat.getDateTimeInstance( DateFormat.LONG, DateFormat.LONG );
	private Calendar cal = new GregorianCalendar( 2021, 1, 31, 17, 30, 0 );

	@Test
	public void testCompositeIdSimple(SessionFactoryScope scope) {
		PurchaseRecord record = new PurchaseRecord();
		scope.inTransaction(
				session -> {
					// persist the record to get the id generated
					record.setTimestamp( cal.getTime() );
					session.persist( record );
				}
		);

		// test that the id was generated
		PurchaseRecord.Id generatedId = record.getId();
		Date timestamp = record.getTimestamp();
		assertThat( generatedId ).isNotNull();
		assertThat( generatedId.getPurchaseSequence() ).isNotNull();
		assertTrue( generatedId.getPurchaseNumber() > 0 );

		PurchaseRecord find1 = scope.fromTransaction(
				session -> {
					// find the record, and see that the ids match
					PurchaseRecord find = session.get( PurchaseRecord.class, generatedId );
					assertThat( find ).isNotNull();
					assertThat( find.getId() ).isEqualTo( generatedId );
					assertThat( df.format( find.getTimestamp() ) ).isEqualTo( df.format( timestamp ) );
					return find;
				}
		);

		PurchaseRecord record2 = new PurchaseRecord();
		scope.inTransaction(
				session -> {
					// generate another new record
					cal.roll( Calendar.SECOND, true );
					record2.setTimestamp( cal.getTime() );
					session.persist( record2 );
				}
		);


		PurchaseRecord.Id generatedId2 = record2.getId();
		Date timestamp2 = record2.getTimestamp();

		PurchaseRecord find2 = scope.fromTransaction(
				session -> {
					return session.get( PurchaseRecord.class, generatedId2 );

				}
		);

		// test that the ids are different
		PurchaseRecord.Id id1 = find1.getId();
		PurchaseRecord.Id id2 = find2.getId();
		String seq1 = id1.getPurchaseSequence();
		String seq2 = id2.getPurchaseSequence();
		int num1 = id1.getPurchaseNumber();
		int num2 = id2.getPurchaseNumber();

		assertThat( df.format( find2.getTimestamp() ) ).isEqualTo( df.format( timestamp2 ) );
		assertThat( id1 ).isNotEqualTo( id2 );
		assertThat( seq1 ).isNotEqualTo( seq2 );
		assertThat( num1 ).isNotEqualTo( num2 );
	}

	@Test
	public void testDetachedProperty(SessionFactoryScope scope) {
		PurchaseRecord record = new PurchaseRecord();
		scope.inTransaction(
				session -> {
					// persist the record
					cal.roll( Calendar.SECOND, true );
					record.setTimestamp( cal.getTime() );
					session.persist( record );
				}
		);

		PurchaseRecord.Id generatedId = record.getId();

		// change a non-id property, but do not persist
		Date persistedTimestamp = record.getTimestamp();
		cal.roll( Calendar.SECOND, true );
		Date newTimestamp = cal.getTime();
		record.setTimestamp( newTimestamp );

		PurchaseRecord find = scope.fromTransaction(
				session ->
						session.get( PurchaseRecord.class, generatedId )
		);

		// see that we get the original id, and the original timestamp
		assertThat( find.getId() ).isEqualTo( generatedId );
		assertThat( df.format( find.getTimestamp() ) ).isEqualTo( df.format( persistedTimestamp ) );

		scope.inTransaction(
				session -> {
					// update with the new timestamp
					session.merge( record );
				}
		);

		// find the newly updated record
		PurchaseRecord find2 = scope.fromTransaction(
				session ->
						session.get( PurchaseRecord.class, generatedId )
		);

		// see that we get the original id, and the new timestamp
		assertThat( find2.getId() ).isEqualTo( generatedId );
		assertThat( df.format( find2.getTimestamp() ) ).isEqualTo( df.format( newTimestamp ) );
	}

	@Test
	public void testDetachedId(SessionFactoryScope scope) {
		Date timestamp1 = cal.getTime();
		cal.roll( Calendar.SECOND, true );
		Date timestamp2 = cal.getTime();
		PurchaseRecord record1 = new PurchaseRecord();
		PurchaseRecord record2 = new PurchaseRecord();

		scope.inTransaction(
				session -> {
					// persist two records
					record1.setTimestamp( timestamp1 );
					record2.setTimestamp( timestamp2 );
					session.persist( record1 );
					session.persist( record2 );
				}
		);

		PurchaseRecord.Id generatedId1 = record1.getId();
		PurchaseRecord.Id generatedId2 = record2.getId();

		// change the ids around - effectively making record1 have the same id as record2
		// do not persist yet
		PurchaseRecord.Id toChangeId1 = new PurchaseRecord.Id();
		toChangeId1.setPurchaseNumber( record2.getId().getPurchaseNumber() );
		toChangeId1.setPurchaseSequence( record2.getId().getPurchaseSequence() );
		record1.setId( toChangeId1 );

		PurchaseRecord find1 = scope.fromTransaction(
				session ->
						session.get( PurchaseRecord.class, generatedId1 )
		);


		PurchaseRecord find2 = scope.fromTransaction(
				session ->
						session.get( PurchaseRecord.class, generatedId2 )
		);

		// see that we get the original ids (and timestamps)
		// i.e. weren't changed by changing the detached object
		assertThat( find1.getId() ).isEqualTo( generatedId1 );
		assertThat( df.format( find1.getTimestamp() ) ).isEqualTo( df.format( timestamp1 ) );
		assertThat( find2.getId() ).isEqualTo( generatedId2 );
		assertThat( df.format( find2.getTimestamp() ) ).isEqualTo( df.format( timestamp2 ) );

		scope.inTransaction(
				session ->
						// update with the new changed record id
						session.merge( record1 )
		);

		// test that record1 did not get a new generated id, and kept record2's id
		PurchaseRecord.Id foundId1 = record1.getId();
		assertThat( toChangeId1 ).isSameAs( foundId1 );
		assertThat( foundId1.getPurchaseNumber() ).isEqualTo( toChangeId1.getPurchaseNumber() );
		assertThat( foundId1.getPurchaseSequence() ).isEqualTo( toChangeId1.getPurchaseSequence() );

		// find record 2 and see that it has the timestamp originally found in record 1
		PurchaseRecord find3 = scope.fromTransaction(
				session ->
						session.get( PurchaseRecord.class, generatedId2 )
		);

		// see that we get the original id (2), and the new timestamp (1)
		assertThat( df.format( find3.getTimestamp() ) ).isEqualTo( df.format( timestamp1 ) );
		assertThat( find3.getId() ).isEqualTo( generatedId2 );
	}

	@Test
	public void testLoad(SessionFactoryScope scope) {
		PurchaseRecord record = new PurchaseRecord();
		scope.inTransaction(
				session -> {
					// persist the record, then get the id and timestamp back
					record.setTimestamp( cal.getTime() );
					session.persist( record );
				}
		);

		PurchaseRecord.Id id = record.getId();
		Date timestamp = record.getTimestamp();

		// using the given id, load a transient record
		PurchaseRecord toLoad = new PurchaseRecord();

		scope.inTransaction(
				session ->
						session.load( toLoad, id )
		);

		// show that the correct timestamp and ids were loaded
		assertThat( toLoad.getId() ).isEqualTo( id );
		assertThat( df.format( toLoad.getTimestamp() ) ).isEqualTo( df.format( timestamp ) );
	}

	@Test
	public void testEvict(SessionFactoryScope scope) {
		Date timestamp1 = cal.getTime();
		cal.roll( Calendar.SECOND, true );
		Date timestamp2 = cal.getTime();

		PurchaseRecord record = new PurchaseRecord();
		scope.inTransaction(
				session -> {
					// persist the record, then evict it, then make changes to it ("within" the session)
					record.setTimestamp( timestamp1 );
					session.persist( record );
					session.flush();
					session.evict( record );

					record.setTimestamp( timestamp2 );
				}
		);

		PurchaseRecord.Id generatedId = record.getId();

		// now, re-fetch the record and show that the timestamp change wasn't persisted
		PurchaseRecord persistent = scope.fromTransaction(
				session ->
						session.get( PurchaseRecord.class, generatedId )
		);

		assertThat( persistent.getId() ).isEqualTo( generatedId );
		assertThat( df.format( persistent.getTimestamp() ) ).isEqualTo( df.format( timestamp1 ) );
	}

	@Test
	public void testMerge(SessionFactoryScope scope) {
		Date timestamp1 = cal.getTime();
		cal.roll( Calendar.SECOND, true );
		Date timestamp2 = cal.getTime();
		PurchaseRecord record = new PurchaseRecord();
		scope.inTransaction(
				session -> {
					// persist the record
					record.setTimestamp( timestamp1 );
					session.persist( record );
				}
		);

		// test that the id was generated
		PurchaseRecord.Id generatedId = record.getId();
		assertThat( generatedId ).isNotNull();
		assertThat( generatedId.getPurchaseSequence() ).isNotNull();

		PurchaseRecord persistent = scope.fromTransaction(
				session -> {
					// update detached object, retrieve persistent object, then merge
					PurchaseRecord detached = record;
					detached.setTimestamp( timestamp2 );
					PurchaseRecord p = session.get( PurchaseRecord.class, generatedId );

					// show that the timestamp hasn't changed
					assertThat( df.format( p.getTimestamp() ) ).isEqualTo( df.format( timestamp1 ) );

					session.merge( detached );
					return p;
				}
		);

		// show that the persistent object was changed only after the session flush
		assertThat( df.format( persistent.getTimestamp() ) ).isEqualTo( df.format( timestamp2 ) );

		// show that the persistent store was updated - not just the in-memory object
		scope.fromTransaction(
				session ->
						session.get( PurchaseRecord.class, generatedId )
		);


		assertThat( df.format( persistent.getTimestamp() ) ).isEqualTo( df.format( timestamp2 ) );
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		PurchaseRecord record = new PurchaseRecord();

		// persist the record
		scope.inTransaction(
				session ->
						session.persist( record )
		);

		PurchaseRecord.Id generatedId = record.getId();

		// re-fetch, then delete the record
		scope.inTransaction(
				session -> {
					PurchaseRecord find = session.get( PurchaseRecord.class, generatedId );
					session.remove( find );
					assertFalse( session.contains( find ) );
				}
		);

		// attempt to re-fetch - show it was deleted
		PurchaseRecord find = scope.fromTransaction(
				session ->
						session.get( PurchaseRecord.class, generatedId )
		);

		assertThat( find ).isNull();
	}

	@Test
	public void testGeneratedIdsWithChildren(SessionFactoryScope scope) {
		// set up the record and details
		PurchaseRecord record = new PurchaseRecord();
		Set details = record.getDetails();
		details.add( new PurchaseDetail( record, "p@1", 1 ) );
		details.add( new PurchaseDetail( record, "p@2", 2 ) );

		scope.inTransaction(
				session ->
						session.persist( record )
		);

		// show that the ids were generated (non-zero) and come out the same
		int foundPurchaseNumber = record.getId().getPurchaseNumber();
		String foundPurchaseSequence = record.getId().getPurchaseSequence();
		assertThat( record.getId() ).isNotNull();
		assertThat( foundPurchaseNumber ).isGreaterThan( 0 );
		assertThat( foundPurchaseSequence ).isNotNull();

		// search on detail1 by itself and show it got the parent's id

		// doAfterTransactionCompletion a find to show that it will wire together fine
		PurchaseRecord foundRecord = scope.fromTransaction(
				session ->
						session.get(
								PurchaseRecord.class,
								new PurchaseRecord.Id( foundPurchaseNumber, foundPurchaseSequence )
						)
		);

		// some simple test to see it fetched
		assertThat( foundRecord.getDetails().size() ).isEqualTo( 2 );
	}

}
