/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.immutable.entitywithmutablecollection;

import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.QueryException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
@SuppressWarnings({ "UnusedDeclaration" })
public abstract class AbstractEntityWithOneToManyTest extends BaseCoreFunctionalTestCase {
	private boolean isContractPartiesInverse;
	private boolean isContractPartiesBidirectional;
	private boolean isContractVariationsBidirectional;
	private boolean isContractVersioned;

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	protected boolean checkUpdateCountsAfterAddingExistingElement() {
		return true;
	}

	protected boolean checkUpdateCountsAfterRemovingElementWithoutDelete() {
		return true;
	}

	protected void prepareTest() throws Exception {
		super.prepareTest();
		isContractPartiesInverse = sessionFactory().getCollectionPersister( Contract.class.getName() + ".parties" )
				.isInverse();
		try {
			sessionFactory().getEntityPersister( Party.class.getName() ).getPropertyType( "contract" );
			isContractPartiesBidirectional = true;
		}
		catch (QueryException ex) {
			isContractPartiesBidirectional = false;
		}
		try {
			sessionFactory().getEntityPersister( ContractVariation.class.getName() ).getPropertyType( "contract" );
			isContractVariationsBidirectional = true;
		}
		catch (QueryException ex) {
			isContractVariationsBidirectional = false;
		}

		isContractVersioned = sessionFactory().getEntityPersister( Contract.class.getName() ).isVersioned();
	}

	@Test
	public void testUpdateProperty() {
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		contract.addParty( new Party( "party" ) );
		inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );

//					Contract c = (Contract) s.createCriteria( Contract.class ).uniqueResult();
					c.setCustomerName( "yogi" );
					assertEquals( 1, c.getParties().size() );
					Party party = (Party) c.getParties().iterator().next();
					party.setName( "new party" );
				}
		);

		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
//					c = (Contract) s.createCriteria( Contract.class ).uniqueResult();
					assertEquals( 1, c.getParties().size() );
					Party party = (Party) c.getParties().iterator().next();
					assertEquals( "party", party.getName() );
					if ( isContractPartiesBidirectional ) {
						assertSame( c, party.getContract() );
					}
					s.delete( c );

					assertPartyAndContractAreDeleted( s );

				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testCreateWithNonEmptyOneToManyCollectionOfNew() {
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		contract.addParty( new Party( "party" ) );
		inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 1, c.getParties().size() );
					Party party = (Party) c.getParties().iterator().next();
					assertEquals( "party", party.getName() );
					if ( isContractPartiesBidirectional ) {
						assertSame( c, party.getContract() );
					}
					s.delete( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testCreateWithNonEmptyOneToManyCollectionOfExisting() {
		clearCounts();

		Party firstParty = new Party( "party" );
		inTransaction(
				s -> s.persist( firstParty )
		);

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		contract.addParty( firstParty );
		inTransaction(
				s -> s.save( contract )
		);

		assertInsertCount( 1 );
		// BUG, should be assertUpdateCount( ! isContractPartiesInverse && isPartyVersioned ? 1 : 0 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					if ( isContractPartiesInverse ) {
						assertEquals( 0, c.getParties().size() );
						Party party = getParty( s );
						assertNull( party.getContract() );
						s.delete( party );
					}
					else {
						assertEquals( 1, c.getParties().size() );
						Party party = (Party) c.getParties().iterator().next();
						assertEquals( "party", party.getName() );
						if ( isContractPartiesBidirectional ) {
							assertSame( c, party.getContract() );
						}
					}
					s.delete( c );
					assertPartyAndContractAreDeleted( s );

				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testAddNewOneToManyElementToPersistentEntity() {
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = s.get( Contract.class, contract.getId() );
					assertEquals( 0, c.getParties().size() );
					c.addParty( new Party( "party" ) );
				}
		);

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 1, c.getParties().size() );
					Party party = (Party) c.getParties().iterator().next();
					assertEquals( "party", party.getName() );
					if ( isContractPartiesBidirectional ) {
						assertSame( c, party.getContract() );
					}
					s.delete( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testAddExistingOneToManyElementToPersistentEntity() {
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );
		inTransaction(
				s -> {
					s.persist( contract );
					s.persist( firstParty );
				}
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = s.get( Contract.class, contract.getId() );
					assertEquals( 0, c.getParties().size() );
					Party party = s.get( Party.class, firstParty.getId() );
					if ( isContractPartiesBidirectional ) {
						assertNull( party.getContract() );
					}
					c.addParty( party );
				}
		);

		assertInsertCount( 0 );
		if ( checkUpdateCountsAfterAddingExistingElement() ) {
			assertUpdateCount( isContractVersioned && !isContractPartiesInverse ? 1 : 0 );
		}
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					if ( isContractPartiesInverse ) {
						assertEquals( 0, c.getParties().size() );
						s.delete( firstParty );
					}
					else {
						assertEquals( 1, c.getParties().size() );
						Party party = (Party) c.getParties().iterator().next();
						assertEquals( "party", party.getName() );
						if ( isContractPartiesBidirectional ) {
							assertSame( c, party.getContract() );
						}
					}
					s.delete( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testCreateWithEmptyOneToManyCollectionUpdateWithExistingElement() {
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );

		inTransaction(
				s -> {
					s.persist( contract );
					s.persist( firstParty );
				}

		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		contract.addParty( firstParty );

		inTransaction(
				s -> s.update( contract )
		);

		assertInsertCount( 0 );
		if ( checkUpdateCountsAfterAddingExistingElement() ) {
			assertUpdateCount( isContractVersioned && !isContractPartiesInverse ? 1 : 0 );
		}
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					if ( isContractPartiesInverse ) {
						assertEquals( 0, c.getParties().size() );
						s.delete( firstParty );
					}
					else {
						assertEquals( 1, c.getParties().size() );
						Party party = (Party) c.getParties().iterator().next();
						assertEquals( "party", party.getName() );
						if ( isContractPartiesBidirectional ) {
							assertSame( c, party.getContract() );
						}
					}
					s.delete( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testCreateWithNonEmptyOneToManyCollectionUpdateWithNewElement() {
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );
		contract.addParty( firstParty );

		inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		Party newParty = new Party( "new party" );
		contract.addParty( newParty );

		inTransaction(
				s -> s.update( contract )
		);

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 2, c.getParties().size() );
					for ( Object o : c.getParties() ) {
						Party aParty = (Party) o;
						if ( aParty.getId() == firstParty.getId() ) {
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
					s.delete( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testCreateWithEmptyOneToManyCollectionMergeWithExistingElement() {
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );

		inTransaction(
				s -> s.persist( firstParty )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		contract.addParty( firstParty );

		inTransaction(
				s -> s.merge( contract )
		);

		assertInsertCount( 0 );
		if ( checkUpdateCountsAfterAddingExistingElement() ) {
			assertUpdateCount( isContractVersioned && !isContractPartiesInverse ? 1 : 0 );
		}
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					if ( isContractPartiesInverse ) {
						assertEquals( 0, c.getParties().size() );
						s.delete( firstParty );
					}
					else {
						assertEquals( 1, c.getParties().size() );
						Party party = (Party) c.getParties().iterator().next();
						assertEquals( "party", party.getName() );
						if ( isContractPartiesBidirectional ) {
							assertSame( c, party.getContract() );
						}
					}
					s.delete( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testCreateWithNonEmptyOneToManyCollectionMergeWithNewElement() {
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );
		contract.addParty( firstParty );

		inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		Party newParty = new Party( "new party" );
		contract.addParty( newParty );

		inTransaction(
				s -> s.merge( contract )
		);

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 2, c.getParties().size() );
					for ( Object o : c.getParties() ) {
						Party aParty = (Party) o;
						if ( aParty.getId() == firstParty.getId() ) {
							assertEquals( "party", aParty.getName() );
						}
						else if ( !aParty.getName().equals( newParty.getName() ) ) {
							fail( "unknown party:" + aParty.getName() );
						}
						if ( isContractPartiesBidirectional ) {
							assertSame( c, aParty.getContract() );
						}
					}
					s.delete( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testMoveOneToManyElementToNewEntityCollection() {
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		contract.addParty( new Party( "party" ) );

		inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		Contract contract2 = new Contract( null, "david", "phone" );

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 1, c.getParties().size() );
					Party party = (Party) c.getParties().iterator().next();
					assertEquals( "party", party.getName() );
					if ( isContractPartiesBidirectional ) {
						assertSame( c, party.getContract() );
					}
					c.removeParty( party );
					contract2.addParty( party );
					s.save( contract2 );
				}
		);

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContractById( s, contract.getId() );
					Contract c2 = getContractById( s, contract2.getId() );
					if ( isContractPartiesInverse ) {
						assertEquals( 1, c.getParties().size() );
						Party party = (Party) c.getParties().iterator().next();
						assertEquals( "party", party.getName() );
						if ( isContractPartiesBidirectional ) {
							assertSame( c, party.getContract() );
						}
						assertEquals( 0, c2.getParties().size() );
					}
					else {
						assertEquals( 0, c.getParties().size() );
						assertEquals( 1, c2.getParties().size() );
						Party party = (Party) c2.getParties().iterator().next();
						assertEquals( "party", party.getName() );
						if ( isContractPartiesBidirectional ) {
							assertSame( c2, party.getContract() );
						}
					}
					s.delete( c );
					s.delete( c2 );
					assertPartyAndContractAreDeleted( s );

				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testMoveOneToManyElementToExistingEntityCollection() {
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		contract.addParty( new Party( "party" ) );
		Contract contract2 = new Contract( null, "david", "phone" );

		inTransaction(
				s -> {
					s.persist( contract );
					s.persist( contract2 );
				}
		);

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContractById( s, contract.getId() );
					assertEquals( 1, c.getParties().size() );
					Party party = (Party) c.getParties().iterator().next();
					assertEquals( "party", party.getName() );
					if ( isContractPartiesBidirectional ) {
						assertSame( c, party.getContract() );
					}
					c.removeParty( party );
					Contract c2 = getContractById( s, contract2.getId() );
					c2.addParty( party );
				}
		);

		assertInsertCount( 0 );
		assertUpdateCount( isContractVersioned ? 2 : 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContractById( s, contract.getId() );
					Contract c2 = getContractById( s, contract2.getId() );
					if ( isContractPartiesInverse ) {
						assertEquals( 1, c.getParties().size() );
						Party party = (Party) c.getParties().iterator().next();
						assertEquals( "party", party.getName() );
						if ( isContractPartiesBidirectional ) {
							assertSame( c, party.getContract() );
						}
						assertEquals( 0, c2.getParties().size() );
					}
					else {
						assertEquals( 0, c.getParties().size() );
						assertEquals( 1, c2.getParties().size() );
						Party party = (Party) c2.getParties().iterator().next();
						assertEquals( "party", party.getName() );
						if ( isContractPartiesBidirectional ) {
							assertSame( c2, party.getContract() );
						}
					}
					s.delete( c );
					s.delete( c2 );
					assertPartyAndContractAreDeleted( s );

				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testRemoveOneToManyElementUsingUpdate() {
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );
		contract.addParty( firstParty );

		inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		contract.removeParty( firstParty );
		assertEquals( 0, contract.getParties().size() );
		if ( isContractPartiesBidirectional ) {
			assertNull( firstParty.getContract() );
		}

		inTransaction(
				s -> {
					s.update( contract );
					s.update( firstParty );
				}
		);

		if ( checkUpdateCountsAfterRemovingElementWithoutDelete() ) {
			assertUpdateCount( isContractVersioned && !isContractPartiesInverse ? 1 : 0 );
		}
		assertDeleteCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					if ( isContractPartiesInverse ) {
						assertEquals( 1, c.getParties().size() );
						Party party = (Party) c.getParties().iterator().next();
						assertEquals( "party", party.getName() );
						assertSame( c, party.getContract() );
					}
					else {
						assertEquals( 0, c.getParties().size() );
						Party party = getParty( s );
						if ( isContractPartiesBidirectional ) {
							assertNull( party.getContract() );
						}
						s.delete( party );
					}
					s.delete( c );
					assertPartyAndContractAreDeleted( s );

				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testRemoveOneToManyElementUsingMerge() {
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );
		contract.addParty( firstParty );

		inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		contract.removeParty( firstParty );
		assertEquals( 0, contract.getParties().size() );
		if ( isContractPartiesBidirectional ) {
			assertNull( firstParty.getContract() );
		}

		inTransaction(
				s -> {
					s.merge( contract );
					s.merge( firstParty );
				}
		);


		if ( checkUpdateCountsAfterRemovingElementWithoutDelete() ) {
			assertUpdateCount( isContractVersioned && !isContractPartiesInverse ? 1 : 0 );
		}
		assertDeleteCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					if ( isContractPartiesInverse ) {
						assertEquals( 1, c.getParties().size() );
						Party party = (Party) c.getParties().iterator().next();
						assertEquals( "party", party.getName() );
						assertSame( c, party.getContract() );
					}
					else {
						assertEquals( 0, c.getParties().size() );
						Party party = getParty( s );
						if ( isContractPartiesBidirectional ) {
							assertNull( party.getContract() );
						}
						s.delete( party );
					}
					s.delete( c );
					assertPartyAndContractAreDeleted( s );
				}

		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testDeleteOneToManyElement() {
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );
		contract.addParty( firstParty );

		inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					s.update( contract );
					contract.removeParty( firstParty );
					s.delete( firstParty );
				}
		);

		assertUpdateCount( isContractVersioned ? 1 : 0 );
		assertDeleteCount( 1 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 0, c.getParties().size() );
					Party party = getParty( s );
					assertNull( party );
					s.delete( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
	}

	@Test
	public void testRemoveOneToManyElementByDelete() {
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );
		contract.addParty( firstParty );

		inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		contract.removeParty( firstParty );
		assertEquals( 0, contract.getParties().size() );
		if ( isContractPartiesBidirectional ) {
			assertNull( firstParty.getContract() );
		}

		inTransaction(
				s -> {
					s.update( contract );
					s.delete( firstParty );
				}
		);

		assertUpdateCount( isContractVersioned ? 1 : 0 );
		assertDeleteCount( 1 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 0, c.getParties().size() );
					s.delete( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
	}

	@Test
	public void testRemoveOneToManyOrphanUsingUpdate() {
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		ContractVariation contractV = new ContractVariation( 1, contract );
		contractV.setText( "cv1" );

		inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		contract.getVariations().remove( contractV );
		contractV.setContract( null );
		assertEquals( 0, contract.getVariations().size() );
		if ( isContractVariationsBidirectional ) {
			assertNull( contractV.getContract() );
		}

		inTransaction(
				s -> s.update( contract )
		);

		assertUpdateCount( isContractVersioned ? 1 : 0 );
		assertDeleteCount( 1 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 0, c.getVariations().size() );
					ContractVariation cv = getContractVariation( s );
					assertNull( cv );
					s.delete( c );
					assertPartyAndContractAreDeleted( s );

				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
	}

	@Test
	public void testRemoveOneToManyOrphanUsingMerge() {
		Contract contract = new Contract( null, "gail", "phone" );
		ContractVariation contractVariation = new ContractVariation( 1, contract );

		inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		contract.getVariations().remove( contractVariation );
		contractVariation.setContract( null );
		assertEquals( 0, contract.getVariations().size() );
		if ( isContractVariationsBidirectional ) {
			assertNull( contractVariation.getContract() );
		}

		inTransaction(
				s -> {
					s.merge( contract );
					s.merge( contractVariation );
				}
		);

		assertUpdateCount( isContractVersioned ? 1 : 0 );
		assertDeleteCount( 1 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 0, c.getVariations().size() );
					ContractVariation cv = getContractVariation( s );
					assertNull( cv );
					s.delete( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
	}

	@Test
	public void testDeleteOneToManyOrphan() {
		clearCounts();

		Contract contract = new Contract( null, "gail", "phone" );
		ContractVariation contractVariation = new ContractVariation( 1, contract );

		inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					s.update( contract );
					contract.getVariations().remove( contractVariation );
					contractVariation.setContract( null );
					assertEquals( 0, contract.getVariations().size() );
					s.delete( contractVariation );
				}
		);

		assertUpdateCount( isContractVersioned ? 1 : 0 );
		assertDeleteCount( 1 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 0, c.getVariations().size() );
					ContractVariation cv = getContractVariation( s );
					assertNull( cv );
					s.delete( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
	}

	@Test
	public void testOneToManyCollectionOptimisticLockingWithMerge() {
		clearCounts();

		Contract cOrig = new Contract( null, "gail", "phone" );
		Party partyOrig = new Party( "party" );
		cOrig.addParty( partyOrig );

		inTransaction(
				s -> s.persist( cOrig )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = s.get( Contract.class, cOrig.getId() );
					Party newParty = new Party( "new party" );
					c.addParty( newParty );
				}
		);

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		inSession(
				s -> {
					cOrig.removeParty( partyOrig );
					try {
						s.merge( cOrig );
						assertFalse( isContractVersioned );
					}
					catch (PersistenceException ex) {
						assertTyping( StaleObjectStateException.class, ex.getCause() );
						assertTrue( isContractVersioned );
					}
					finally {
						s.getTransaction().rollback();
					}
				}
		);


		inTransaction(
				s -> {
					Contract c = getContract( s );
					s.delete( c );
					assertPartyAndContractAreDeleted( s );

				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testOneToManyCollectionOptimisticLockingWithUpdate() {
		clearCounts();

		Contract cOrig = new Contract( null, "gail", "phone" );
		Party partyOrig = new Party( "party" );
		cOrig.addParty( partyOrig );
		inTransaction(
				s -> s.persist( cOrig )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Contract c = s.get( Contract.class, cOrig.getId() );
					Party newParty = new Party( "new party" );
					c.addParty( newParty );
				}
		);

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		inSession(
				s -> {
					cOrig.removeParty( partyOrig );
					s.update( cOrig );
					try {
						s.getTransaction().commit();
						assertFalse( isContractVersioned );
					}
					catch (PersistenceException ex) {
						s.getTransaction().rollback();
						assertTrue( isContractVersioned );
						if ( !sessionFactory().getSessionFactoryOptions().isJdbcBatchVersionedData() ) {
							assertTyping( StaleObjectStateException.class, ex.getCause() );
						}
						else {
							assertTyping( StaleStateException.class, ex.getCause() );
						}
					}
				}
		);

		inTransaction(
				s -> {
					Contract c = getContract( s );
					s.createQuery( "delete from Party" ).executeUpdate();
					s.delete( c );
					assertPartyAndContractAreDeleted( s );
				}
		);
	}

	private Contract getContractById(SessionImplementor s, long id) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Contract> criteria = criteriaBuilder.createQuery( Contract.class );
		Root<Contract> root = criteria.from( Contract.class );
		criteria.where( criteriaBuilder.equal( root.get( "id" ), id ) );
		return s.createQuery( criteria ).uniqueResult();
	}

	private Contract getContract(SessionImplementor s) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Contract> criteria = criteriaBuilder.createQuery( Contract.class );
		criteria.from( Contract.class );
		return s.createQuery( criteria ).uniqueResult();
	}

	private ContractVariation getContractVariation(SessionImplementor s) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<ContractVariation> criteria = criteriaBuilder.createQuery( ContractVariation.class );
		criteria.from( ContractVariation.class );
		return s.createQuery( criteria ).uniqueResult();
	}

	private Party getParty(SessionImplementor s) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Party> criteria = criteriaBuilder.createQuery( Party.class );
		criteria.from( Party.class );
		return s.createQuery( criteria ).uniqueResult();
	}

	private void assertPartyAndContractAreDeleted(SessionImplementor s) {
		assertEquals( Long.valueOf( 0 ), getContractRowCount( s ) );
		assertEquals( Long.valueOf( 0 ), getPartyRowCount( s ) );
	}

	private Long getContractRowCount(SessionImplementor s) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Long> rowCountCriteria = criteriaBuilder.createQuery( Long.class );
		Root<Contract> root = rowCountCriteria.from( Contract.class );
		rowCountCriteria.select( criteriaBuilder.count( root ) );
		return s.createQuery( rowCountCriteria ).uniqueResult();
	}

	private Long getPartyRowCount(SessionImplementor s) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Long> rowCountCriteria = criteriaBuilder.createQuery( Long.class );
		Root<Party> root = rowCountCriteria.from( Party.class );
		rowCountCriteria.select( criteriaBuilder.count( root ) );
		return s.createQuery( rowCountCriteria ).uniqueResult();
	}

	protected void clearCounts() {
		sessionFactory().getStatistics().clear();
	}

	protected void assertInsertCount(int expected) {
		int inserts = (int) sessionFactory().getStatistics().getEntityInsertCount();
		assertEquals( "unexpected insert count", expected, inserts );
	}

	protected void assertUpdateCount(int expected) {
		int updates = (int) sessionFactory().getStatistics().getEntityUpdateCount();
		assertEquals( "unexpected update counts", expected, updates );
	}

	protected void assertDeleteCount(int expected) {
		int deletes = (int) sessionFactory().getStatistics().getEntityDeleteCount();
		assertEquals( "unexpected delete counts", expected, deletes );
	}
}
