/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.immutable.entitywithmutablecollection;
import org.junit.Test;

import org.hibernate.QueryException;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
@SuppressWarnings( {"UnusedDeclaration"})
public abstract class AbstractEntityWithOneToManyTest extends BaseCoreFunctionalTestCase {
	private boolean isContractPartiesInverse;
	private boolean isContractPartiesBidirectional;
	private boolean isContractVariationsBidirectional;
	private boolean isContractVersioned;
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true");
	}

	protected boolean checkUpdateCountsAfterAddingExistingElement() {
		return true;
	}

	protected boolean checkUpdateCountsAfterRemovingElementWithoutDelete() {
		return true;
	}

	protected void prepareTest() throws Exception {
		super.prepareTest();
		isContractPartiesInverse = sessionFactory().getCollectionPersister( Contract.class.getName() + ".parties" ).isInverse();
		try {
			 sessionFactory().getEntityPersister( Party.class.getName() ).getPropertyType( "contract" );
			isContractPartiesBidirectional = true;
		}
		catch ( QueryException ex) {
			isContractPartiesBidirectional = false;
		}
		try {
			 sessionFactory().getEntityPersister( ContractVariation.class.getName() ).getPropertyType( "contract" );
			isContractVariationsBidirectional = true;
		}
		catch ( QueryException ex) {
			isContractVariationsBidirectional = false;
		}

		isContractVersioned = sessionFactory().getEntityPersister( Contract.class.getName() ).isVersioned();
	}

	@Test
	public void testUpdateProperty() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone");
		c.addParty( new Party( "party" ) );
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria( Contract.class ).uniqueResult();
		c.setCustomerName( "yogi" );
		assertEquals( 1, c.getParties().size() );
		Party party = ( Party ) c.getParties().iterator().next();
		party.setName( "new party" );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria( Contract.class ).uniqueResult();
		assertEquals( 1, c.getParties().size() );
		party = ( Party ) c.getParties().iterator().next();
		assertEquals( "party", party.getName() );
		if ( isContractPartiesBidirectional ) {
			assertSame( c, party.getContract() );
		}
		s.delete(c);
		assertEquals( Long.valueOf( 0 ), s.createCriteria( Contract.class ).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria( Party.class ).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testCreateWithNonEmptyOneToManyCollectionOfNew() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone");
		c.addParty( new Party( "party" ) );
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria( Contract.class ).uniqueResult();
		assertEquals( 1, c.getParties().size() );
		Party party = ( Party ) c.getParties().iterator().next();
		assertEquals( "party", party.getName() );
		if ( isContractPartiesBidirectional ) {
			assertSame( c, party.getContract() );
		}
		s.delete(c);
		assertEquals( Long.valueOf( 0 ), s.createCriteria( Contract.class ).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria( Party.class ).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testCreateWithNonEmptyOneToManyCollectionOfExisting() {
		clearCounts();

		Party party = new Party( "party" );
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( party );
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		Contract c = new Contract( null, "gail", "phone");
		c.addParty( party );
		s = openSession();
		t = s.beginTransaction();
		s.save( c );
		t.commit();
		s.close();

		assertInsertCount( 1 );
		// BUG, should be assertUpdateCount( ! isContractPartiesInverse && isPartyVersioned ? 1 : 0 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria( Contract.class ).uniqueResult();
		if ( isContractPartiesInverse ) {
			assertEquals( 0 , c.getParties().size() );
			party = ( Party ) s.createCriteria( Party.class ).uniqueResult();
			assertNull( party.getContract() );
			s.delete( party );
		}
		else {
			assertEquals( 1 , c.getParties().size() );
			party = ( Party ) c.getParties().iterator().next();
			assertEquals( "party", party.getName() );
			if ( isContractPartiesBidirectional ) {
				assertSame( c, party.getContract() );
			}
		}
		s.delete(c);
		assertEquals( Long.valueOf( 0 ), s.createCriteria( Contract.class ).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria( Party.class ).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testAddNewOneToManyElementToPersistentEntity() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone" );
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( c );
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = ( Contract ) s.get( Contract.class, c.getId() );
		assertEquals( 0, c.getParties().size() );
		c.addParty( new Party( "party" ) );
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria( Contract.class ).uniqueResult();
		assertEquals( 1, c.getParties().size() );
		Party party = ( Party ) c.getParties().iterator().next();
		assertEquals( "party", party.getName() );
		if ( isContractPartiesBidirectional ) {
			assertSame( c, party.getContract() );
		}
		s.delete(c);
		assertEquals( Long.valueOf( 0 ), s.createCriteria( Contract.class ).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria( Party.class ).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testAddExistingOneToManyElementToPersistentEntity() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone" );
		Party party = new Party( "party" );
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( c );
		s.persist( party );
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = ( Contract ) s.get( Contract.class, c.getId() );
		assertEquals( 0, c.getParties().size() );
		party = ( Party ) s.get( Party.class, party.getId() );
		if ( isContractPartiesBidirectional ) {
			assertNull( party.getContract() );
		}
		c.addParty( party );
		t.commit();
		s.close();

		assertInsertCount( 0 );
		if ( checkUpdateCountsAfterAddingExistingElement() ) {
			assertUpdateCount( isContractVersioned && ! isContractPartiesInverse ? 1 : 0 );
		}
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria( Contract.class ).uniqueResult();
		if ( isContractPartiesInverse ) {
			assertEquals( 0, c.getParties().size() );
			s.delete( party );
		}
		else {
			assertEquals( 1, c.getParties().size() );
			party = ( Party ) c.getParties().iterator().next();
			assertEquals( "party", party.getName() );
			if ( isContractPartiesBidirectional ) {
				assertSame( c, party.getContract() );
			}
		}
		s.delete(c);
		assertEquals( Long.valueOf( 0 ), s.createCriteria( Contract.class ).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria( Party.class ).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testCreateWithEmptyOneToManyCollectionUpdateWithExistingElement() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone");
		Party party = new Party( "party" );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( c );
		s.persist( party );
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		c.addParty( party );

		s = openSession();
		t = s.beginTransaction();
		s.update( c );
		t.commit();
		s.close();

		assertInsertCount( 0 );
		if ( checkUpdateCountsAfterAddingExistingElement() ) {
			assertUpdateCount( isContractVersioned && ! isContractPartiesInverse ? 1 : 0 );
		}
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		if ( isContractPartiesInverse ) {
			assertEquals( 0, c.getParties().size() );
			s.delete( party );
		}
		else {
			assertEquals( 1, c.getParties().size() );
			party = ( Party ) c.getParties().iterator().next();
			assertEquals( "party", party.getName() );
			if ( isContractPartiesBidirectional ) {
				assertSame( c, party.getContract() );
			}
		}
		s.delete(c);
		assertEquals( Long.valueOf( 0 ), s.createCriteria( Contract.class ).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria( Party.class ).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testCreateWithNonEmptyOneToManyCollectionUpdateWithNewElement() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone");
		Party party = new Party( "party" );
		c.addParty( party );
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		Party newParty = new Party( "new party" );
		c.addParty( newParty );

		s = openSession();
		t = s.beginTransaction();
		s.update( c );
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( 2, c.getParties().size() );
		for ( Object o : c.getParties() ) {
			Party aParty = (Party) o;
			if ( aParty.getId() == party.getId() ) {
				assertEquals( "party", aParty.getName() );
			}
			else if ( aParty.getId() == newParty.getId() ) {
				assertEquals( "new party", aParty.getName() );
			}
			else {
				fail( "unknown party" );
			}
			if ( isContractPartiesBidirectional ) {
				assertSame( c, aParty.getContract() );
			}
		}
		s.delete(c);
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Party.class).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testCreateWithEmptyOneToManyCollectionMergeWithExistingElement() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone");
		Party party = new Party( "party" );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( c );
		s.persist( party );
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		c.addParty( party );

		s = openSession();
		t = s.beginTransaction();
		c = ( Contract ) s.merge( c );
		t.commit();
		s.close();

		assertInsertCount( 0 );
		if ( checkUpdateCountsAfterAddingExistingElement() ) {
			assertUpdateCount( isContractVersioned && ! isContractPartiesInverse ? 1 : 0 );
		}
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria( Contract.class ).uniqueResult();
		if ( isContractPartiesInverse ) {
			assertEquals( 0, c.getParties().size() );
			s.delete( party );
		}
		else {
			assertEquals( 1, c.getParties().size() );
			party = ( Party ) c.getParties().iterator().next();
			assertEquals( "party", party.getName() );
			if ( isContractPartiesBidirectional ) {
				assertSame( c, party.getContract() );
			}
		}
		s.delete(c);
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Party.class).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testCreateWithNonEmptyOneToManyCollectionMergeWithNewElement() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone");
		Party party = new Party( "party" );
		c.addParty( party );
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		Party newParty = new Party( "new party" );
		c.addParty( newParty );

		s = openSession();
		t = s.beginTransaction();
		c = ( Contract ) s.merge( c );
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		assertEquals( 2, c.getParties().size() );
		for ( Object o : c.getParties() ) {
			Party aParty = (Party) o;
			if ( aParty.getId() == party.getId() ) {
				assertEquals( "party", aParty.getName() );
			}
			else if ( !aParty.getName().equals( newParty.getName() ) ) {
				fail( "unknown party:" + aParty.getName() );
			}
			if ( isContractPartiesBidirectional ) {
				assertSame( c, aParty.getContract() );
			}
		}
		s.delete(c);
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Party.class).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testMoveOneToManyElementToNewEntityCollection() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone");
		c.addParty( new Party( "party" ) );
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(c);
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria( Contract.class ).uniqueResult();
		assertEquals( 1, c.getParties().size() );
		Party party = ( Party ) c.getParties().iterator().next();
		assertEquals( "party", party.getName() );
		if ( isContractPartiesBidirectional ) {
			assertSame( c, party.getContract() );
		}
		c.removeParty( party );
		Contract c2 = new Contract(null, "david", "phone" );
		c2.addParty( party );
		s.save( c2 );
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria( Contract.class ).add( Restrictions.idEq( Long.valueOf( c.getId() ) )).uniqueResult();
		c2 = (Contract) s.createCriteria( Contract.class ).add( Restrictions.idEq( Long.valueOf( c2.getId() ) )).uniqueResult();
		if ( isContractPartiesInverse ) {
			assertEquals( 1, c.getParties().size() );
			party = ( Party ) c.getParties().iterator().next();
			assertEquals( "party", party.getName() );
			if ( isContractPartiesBidirectional ) {
				assertSame( c, party.getContract() );
			}
			assertEquals( 0, c2.getParties().size() );
		}
		else {
			assertEquals( 0, c.getParties().size() );
			assertEquals( 1, c2.getParties().size() );
			party = ( Party ) c2.getParties().iterator().next();
			assertEquals( "party", party.getName() );
			if ( isContractPartiesBidirectional ) {
				assertSame( c2, party.getContract() );
			}
		}
		s.delete(c);
		s.delete( c2 );
		assertEquals( new Long( 0 ), s.createCriteria( Contract.class ).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( new Long( 0 ), s.createCriteria( Party.class ).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testMoveOneToManyElementToExistingEntityCollection() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone");
		c.addParty( new Party( "party" ) );
		Contract c2 = new Contract(null, "david", "phone" );
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( c );
		s.persist( c2 );
		t.commit();
		s.close();

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria( Contract.class ).add( Restrictions.idEq( Long.valueOf( c.getId() ) )).uniqueResult();
		assertEquals( 1, c.getParties().size() );
		Party party = ( Party ) c.getParties().iterator().next();
		assertEquals( "party", party.getName() );
		if ( isContractPartiesBidirectional ) {
			assertSame( c, party.getContract() );
		}
		c.removeParty( party );
		c2 = (Contract) s.createCriteria( Contract.class ).add( Restrictions.idEq( Long.valueOf( c2.getId() ) )).uniqueResult();
		c2.addParty( party );
		t.commit();
		s.close();

		assertInsertCount( 0 );
		assertUpdateCount( isContractVersioned ? 2 : 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria( Contract.class ).add( Restrictions.idEq( Long.valueOf( c.getId() ) )).uniqueResult();
		c2 = (Contract) s.createCriteria( Contract.class ).add( Restrictions.idEq( Long.valueOf( c2.getId() ) )).uniqueResult();
		if ( isContractPartiesInverse ) {
			assertEquals( 1, c.getParties().size() );
			party = ( Party ) c.getParties().iterator().next();
			assertEquals( "party", party.getName() );
			if ( isContractPartiesBidirectional ) {
				assertSame( c, party.getContract() );
			}
			assertEquals( 0, c2.getParties().size() );
		}
		else {
			assertEquals( 0, c.getParties().size() );
			assertEquals( 1, c2.getParties().size() );
			party = ( Party ) c2.getParties().iterator().next();
			assertEquals( "party", party.getName() );
			if ( isContractPartiesBidirectional ) {
				assertSame( c2, party.getContract() );
			}
		}
		s.delete(c);
		s.delete( c2 );
		assertEquals( Long.valueOf( 0 ), s.createCriteria( Contract.class ).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria( Party.class ).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testRemoveOneToManyElementUsingUpdate() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone");
		Party party = new Party( "party" );
		c.addParty( party );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( c );
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		c.removeParty( party );
		assertEquals( 0, c.getParties().size() );
		if ( isContractPartiesBidirectional ) {
			assertNull( party.getContract() );
		}

		s = openSession();
		t = s.beginTransaction();
		s.update( c );
		s.update( party );
		t.commit();
		s.close();

		if ( checkUpdateCountsAfterRemovingElementWithoutDelete() ) {
			assertUpdateCount( isContractVersioned && ! isContractPartiesInverse ? 1 : 0 );
		}
		assertDeleteCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = ( Contract ) s.createCriteria( Contract.class ).uniqueResult();
		if ( isContractPartiesInverse ) {
			assertEquals( 1, c.getParties().size() );
			party = ( Party ) c.getParties().iterator().next();
			assertEquals( "party", party.getName() );
			assertSame( c, party.getContract() );
		}
		else {
			assertEquals( 0, c.getParties().size() );
			party = ( Party ) s.createCriteria( Party.class ).uniqueResult();
			if ( isContractPartiesBidirectional ) {
				assertNull( party.getContract() );
			}
			s.delete( party );
		}
		s.delete( c );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Party.class).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testRemoveOneToManyElementUsingMerge() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone");
		Party party = new Party( "party" );
		c.addParty( party );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( c );
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		c.removeParty( party );
		assertEquals( 0, c.getParties().size() );
		if ( isContractPartiesBidirectional ) {
			assertNull( party.getContract() );
		}

		s = openSession();
		t = s.beginTransaction();
		c = ( Contract ) s.merge( c );
		party = ( Party ) s.merge( party );
		t.commit();
		s.close();

		if ( checkUpdateCountsAfterRemovingElementWithoutDelete() ) {
			assertUpdateCount( isContractVersioned && ! isContractPartiesInverse ? 1 : 0 );
		}
		assertDeleteCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = ( Contract ) s.createCriteria( Contract.class ).uniqueResult();
		if ( isContractPartiesInverse ) {
			assertEquals( 1, c.getParties().size() );
			party = ( Party ) c.getParties().iterator().next();
			assertEquals( "party", party.getName() );
			assertSame( c, party.getContract() );
		}
		else {
			assertEquals( 0, c.getParties().size() );
			party = ( Party ) s.createCriteria( Party.class ).uniqueResult();
			if ( isContractPartiesBidirectional ) {
				assertNull( party.getContract() );
			}
			s.delete( party );
		}
		s.delete( c );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Party.class).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testDeleteOneToManyElement() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone");
		Party party = new Party( "party" );
		c.addParty( party );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( c );
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		s.update( c );
		c.removeParty( party );
		s.delete( party );
		t.commit();
		s.close();

		assertUpdateCount( isContractVersioned ? 1 : 0 );
		assertDeleteCount( 1 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = ( Contract ) s.createCriteria( Contract.class ).uniqueResult();
		assertEquals( 0, c.getParties().size() );
		party = ( Party ) s.createCriteria( Party.class ).uniqueResult();
		assertNull( party );
		s.delete( c );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Party.class).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
	}

	@Test
	public void testRemoveOneToManyElementByDelete() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone");
		Party party = new Party( "party" );
		c.addParty( party );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( c );
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		c.removeParty( party );
		assertEquals( 0, c.getParties().size() );
		if ( isContractPartiesBidirectional ) {
			assertNull( party.getContract() );
		}

		s = openSession();
		t = s.beginTransaction();
		s.update( c );
		s.delete( party );
		t.commit();
		s.close();

		assertUpdateCount( isContractVersioned ? 1 : 0 );
		assertDeleteCount( 1 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = ( Contract ) s.createCriteria( Contract.class ).uniqueResult();
		assertEquals( 0, c.getParties().size() );
		s.delete( c );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Party.class).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
	}

	@Test
	public void testRemoveOneToManyOrphanUsingUpdate() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone");
		ContractVariation cv = new ContractVariation( 1, c );
		cv.setText( "cv1" );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( c );
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		c.getVariations().remove( cv );
		cv.setContract( null );
		assertEquals( 0, c.getVariations().size() );
		if ( isContractVariationsBidirectional ) {
			assertNull( cv.getContract() );
		}

		s = openSession();
		t = s.beginTransaction();
		s.update( c );
		t.commit();
		s.close();

		assertUpdateCount( isContractVersioned ? 1 : 0 );
		assertDeleteCount( 1 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = ( Contract ) s.createCriteria( Contract.class ).uniqueResult();
		assertEquals( 0, c.getVariations().size() );
		cv = ( ContractVariation ) s.createCriteria( ContractVariation.class ).uniqueResult();
		assertNull( cv );
		s.delete( c );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
	}

	@Test
	public void testRemoveOneToManyOrphanUsingMerge() {
		Contract c = new Contract( null, "gail", "phone");
		ContractVariation cv = new ContractVariation( 1, c );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( c );
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		c.getVariations().remove( cv );
		cv.setContract( null );
		assertEquals( 0, c.getVariations().size() );
		if ( isContractVariationsBidirectional ) {
			assertNull( cv.getContract() );
		}

		s = openSession();
		t = s.beginTransaction();
		c = ( Contract ) s.merge( c );
		cv = ( ContractVariation ) s.merge( cv );
		t.commit();
		s.close();

		assertUpdateCount( isContractVersioned ? 1 : 0 );
		assertDeleteCount( 1 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = ( Contract ) s.createCriteria( Contract.class ).uniqueResult();
		assertEquals( 0, c.getVariations().size() );
		cv = ( ContractVariation ) s.createCriteria( ContractVariation.class ).uniqueResult();
		assertNull( cv );
		s.delete( c );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
	}

	@Test
	public void testDeleteOneToManyOrphan() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone");
		ContractVariation cv = new ContractVariation( 1, c );

		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist( c );
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		s.update( c );
		c.getVariations().remove( cv );
		cv.setContract( null );
		assertEquals( 0, c.getVariations().size() );
		s.delete( cv );
		t.commit();
		s.close();

		assertUpdateCount( isContractVersioned ? 1 : 0 );
		assertDeleteCount( 1 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		c = ( Contract ) s.createCriteria( Contract.class ).uniqueResult();
		assertEquals( 0, c.getVariations().size() );
		cv = ( ContractVariation ) s.createCriteria( ContractVariation.class ).uniqueResult();
		assertNull( cv );
		s.delete( c );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(ContractVariation.class).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
	}

	@Test
	public void testOneToManyCollectionOptimisticLockingWithMerge() {
		clearCounts();

		Contract cOrig = new Contract( null, "gail", "phone");
		Party partyOrig = new Party( "party" );
		cOrig.addParty( partyOrig );
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(cOrig);
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		Contract c = ( Contract ) s.get( Contract.class, cOrig.getId() );
		Party newParty = new Party( "new party" );
		c.addParty( newParty );
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		cOrig.removeParty( partyOrig );
		try {
			s.merge( cOrig );
			assertFalse( isContractVersioned );
		}
		catch (StaleObjectStateException ex) {
			assertTrue( isContractVersioned);
		}
		finally {
			t.rollback();
		}
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		s.delete(c);
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Party.class).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testOneToManyCollectionOptimisticLockingWithUpdate() {
		clearCounts();

		Contract cOrig = new Contract( null, "gail", "phone");
		Party partyOrig = new Party( "party" );
		cOrig.addParty( partyOrig );
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(cOrig);
		t.commit();
		s.close();

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		Contract c = ( Contract ) s.get( Contract.class, cOrig.getId() );
		Party newParty = new Party( "new party" );
		c.addParty( newParty );
		t.commit();
		s.close();

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		cOrig.removeParty( partyOrig );
		s.update( cOrig );
		try {
			t.commit();
			assertFalse( isContractVersioned );
		}
		catch (StaleStateException ex) {
			t.rollback();
			assertTrue( isContractVersioned );
			if ( ! sessionFactory().getSessionFactoryOptions().isJdbcBatchVersionedData() ) {
				assertTrue( StaleObjectStateException.class.isInstance( ex ) );
			}
		}
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c = (Contract) s.createCriteria(Contract.class).uniqueResult();
		s.createQuery( "delete from Party" ).executeUpdate();
		s.delete( c );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Contract.class).setProjection( Projections.rowCount() ).uniqueResult() );
		assertEquals( Long.valueOf( 0 ), s.createCriteria(Party.class).setProjection( Projections.rowCount() ).uniqueResult() );
		t.commit();
		s.close();
	}

	protected void clearCounts() {
		sessionFactory().getStatistics().clear();
	}

	protected void assertInsertCount(int expected) {
		int inserts = ( int ) sessionFactory().getStatistics().getEntityInsertCount();
		assertEquals( "unexpected insert count", expected, inserts );
	}

	protected void assertUpdateCount(int expected) {
		int updates = ( int ) sessionFactory().getStatistics().getEntityUpdateCount();
		assertEquals( "unexpected update counts", expected, updates );
	}

	protected void assertDeleteCount(int expected) {
		int deletes = ( int ) sessionFactory().getStatistics().getEntityDeleteCount();
		assertEquals( "unexpected delete counts", expected, deletes );
	}
}
