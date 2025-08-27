/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.immutable.entitywithmutablecollection;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.QueryException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.MappingMetamodel;

import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_FETCH_GRAPH;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Gail Badner
 */
@SuppressWarnings("unused")
@SessionFactory(generateStatistics = true)
public abstract class AbstractEntityWithOneToManyTest {
	private boolean isContractPartiesInverse;
	private boolean isContractPartiesBidirectional;
	private boolean isContractVariationsBidirectional;
	private boolean isContractVersioned;

	protected boolean checkUpdateCountsAfterAddingExistingElement() {
		return true;
	}

	protected boolean checkUpdateCountsAfterRemovingElementWithoutDelete() {
		return true;
	}

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) throws Exception {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		MappingMetamodel domainModel = sessionFactory.getRuntimeMetamodels().getMappingMetamodel();

		isContractPartiesInverse = domainModel.getCollectionDescriptor( Contract.class.getName() + ".parties" )
				.isInverse();
		try {
			domainModel.getEntityDescriptor( Party.class.getName() ).getPropertyType( "contract" );
			isContractPartiesBidirectional = true;
		}
		catch (QueryException ex) {
			isContractPartiesBidirectional = false;
		}
		try {
			domainModel.getEntityDescriptor( ContractVariation.class.getName() ).getPropertyType( "contract" );
			isContractVariationsBidirectional = true;
		}
		catch (QueryException ex) {
			isContractVariationsBidirectional = false;
		}

		isContractVersioned = domainModel.getEntityDescriptor( Contract.class.getName() ).isVersioned();
	}

	@Test
	public void testUpdateProperty(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		contract.addParty( new Party( "party" ) );
		scope.inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 , sessionFactory);
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract c = getContract( s );

//					Contract c = (Contract) s.createCriteria( Contract.class ).uniqueResult();
					c.setCustomerName( "yogi" );
					assertEquals( 1, c.getParties().size() );
					Party party = (Party) c.getParties().iterator().next();
					party.setName( "new party" );
				}
		);

		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract c = getContract( s );
//					c = (Contract) s.createCriteria( Contract.class ).uniqueResult();
					assertEquals( 1, c.getParties().size() );
					Party party = (Party) c.getParties().iterator().next();
					assertEquals( "party", party.getName() );
					if ( isContractPartiesBidirectional ) {
						assertSame( c, party.getContract() );
					}
					s.remove( c );

					assertPartyAndContractAreDeleted( s );

				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 2, sessionFactory );
	}

	@Test
	public void testCreateWithNonEmptyOneToManyCollectionOfNew(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		contract.addParty( new Party( "party" ) );
		scope.inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2, sessionFactory );
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 1, c.getParties().size() );
					Party party = (Party) c.getParties().iterator().next();
					assertEquals( "party", party.getName() );
					if ( isContractPartiesBidirectional ) {
						assertSame( c, party.getContract() );
					}
					s.remove( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 2, sessionFactory );
	}

	@Test
	public void testCreateWithNonEmptyOneToManyCollectionOfExisting(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Party firstParty = new Party( "party" );
		scope.inTransaction(
				s -> s.persist( firstParty )
		);

		assertInsertCount( 1, sessionFactory );
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		contract.addParty( firstParty );

		scope.inTransaction(
				s -> {
					s.merge( contract );
				}
		);

		assertInsertCount( 1, sessionFactory );
		// BUG, should be assertUpdateCount( ! isContractPartiesInverse && isPartyVersioned ? 1 : 0 );
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract c = getContract( s );
					if ( isContractPartiesInverse ) {
						assertEquals( 0, c.getParties().size() );
						Party party = getParty( s );
						assertNull( party.getContract() );
						s.remove( party );
					}
					else {
						assertEquals( 1, c.getParties().size() );
						Party party = (Party) c.getParties().iterator().next();
						assertEquals( "party", party.getName() );
						if ( isContractPartiesBidirectional ) {
							assertSame( c, party.getContract() );
						}
					}
					s.remove( c );
					assertPartyAndContractAreDeleted( s );

				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 2, sessionFactory );
	}

	@Test
	public void testAddNewOneToManyElementToPersistentEntity(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		scope.inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 1, sessionFactory );
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract c = s.get( Contract.class, contract.getId() );
					assertEquals( 0, c.getParties().size() );
					c.addParty( new Party( "party" ) );
				}
		);

		assertInsertCount( 1, sessionFactory );
		assertUpdateCount( isContractVersioned ? 1 : 0 , sessionFactory);
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 1, c.getParties().size() );
					Party party = (Party) c.getParties().iterator().next();
					assertEquals( "party", party.getName() );
					if ( isContractPartiesBidirectional ) {
						assertSame( c, party.getContract() );
					}
					s.remove( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 2, sessionFactory );
	}

	@Test
	public void testAddExistingOneToManyElementToPersistentEntity(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );
		scope.inTransaction(
				s -> {
					s.persist( contract );
					s.persist( firstParty );
				}
		);

		assertInsertCount( 2 , sessionFactory);
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
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

		assertInsertCount( 0 , sessionFactory);
		if ( checkUpdateCountsAfterAddingExistingElement() ) {
			assertUpdateCount( isContractVersioned && !isContractPartiesInverse ? 1 : 0, sessionFactory );
		}
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract c = getContract( s );
					if ( isContractPartiesInverse ) {
						assertEquals( 0, c.getParties().size() );
						s.remove( firstParty );
					}
					else {
						assertEquals( 1, c.getParties().size() );
						Party party = (Party) c.getParties().iterator().next();
						assertEquals( "party", party.getName() );
						if ( isContractPartiesBidirectional ) {
							assertSame( c, party.getContract() );
						}
					}
					s.remove( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 2, sessionFactory );
	}

	@Test
	public void testCreateWithEmptyOneToManyCollectionUpdateWithExistingElement(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );

		scope.inTransaction(
				s -> {
					s.persist( contract );
					s.persist( firstParty );
				}

		);

		assertInsertCount( 2 , sessionFactory);
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		contract.addParty( firstParty );

		scope.inTransaction(
				s -> s.merge( contract )
		);

		assertInsertCount( 0 , sessionFactory);
		if ( checkUpdateCountsAfterAddingExistingElement() ) {
			assertUpdateCount( isContractVersioned && !isContractPartiesInverse ? 1 : 0 , sessionFactory);
		}
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract c = getContract( s );
					if ( isContractPartiesInverse ) {
						assertEquals( 0, c.getParties().size() );
						s.remove( firstParty );
					}
					else {
						assertEquals( 1, c.getParties().size() );
						Party party = (Party) c.getParties().iterator().next();
						assertEquals( "party", party.getName() );
						if ( isContractPartiesBidirectional ) {
							assertSame( c, party.getContract() );
						}
					}
					s.remove( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 2, sessionFactory );
	}

	@Test
	public void testCreateWithNonEmptyOneToManyCollectionUpdateWithNewElement(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );
		contract.addParty( firstParty );

		scope.inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 , sessionFactory);
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		Party newParty = new Party( "new party" );
		contract.addParty( newParty );

		scope.inTransaction(
				s -> s.merge( contract )
		);

		assertInsertCount( 1, sessionFactory );
		assertUpdateCount( isContractVersioned ? 1 : 0 , sessionFactory);
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 2, c.getParties().size() );
					for ( Object o : c.getParties() ) {
						Party aParty = (Party) o;
						if ( aParty.getId() == firstParty.getId() ) {
							assertEquals( "party", aParty.getName() );
						}
						else {
							assertEquals( "new party", aParty.getName() );
						}
						if ( isContractPartiesBidirectional ) {
							assertSame( c, aParty.getContract() );
						}
					}
					s.remove( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 3 , sessionFactory);
	}

	@Test
	public void testCreateWithEmptyOneToManyCollectionMergeWithExistingElement(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );

		scope.inTransaction(
				s -> {
					s.persist( contract );
					s.persist( firstParty );
				}
		);

		assertInsertCount( 2 , sessionFactory);
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		contract.addParty( firstParty );

		scope.inTransaction(
				s -> s.merge( contract )
		);

		assertInsertCount( 0 , sessionFactory);
		if ( checkUpdateCountsAfterAddingExistingElement() ) {
			assertUpdateCount( isContractVersioned && !isContractPartiesInverse ? 1 : 0, sessionFactory );
		}
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract c = getContract( s );
					if ( isContractPartiesInverse ) {
						assertEquals( 0, c.getParties().size() );
						s.remove( firstParty );
					}
					else {
						assertEquals( 1, c.getParties().size() );
						Party party = (Party) c.getParties().iterator().next();
						assertEquals( "party", party.getName() );
						if ( isContractPartiesBidirectional ) {
							assertSame( c, party.getContract() );
						}
					}
					s.remove( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 2, sessionFactory );
	}

	@Test
	public void testCreateWithNonEmptyOneToManyCollectionMergeWithNewElement(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );
		contract.addParty( firstParty );

		scope.inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 , sessionFactory);
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		Party newParty = new Party( "new party" );
		contract.addParty( newParty );

		scope.inTransaction(
				s -> s.merge( contract )
		);

		assertInsertCount( 1, sessionFactory );
		assertUpdateCount( isContractVersioned ? 1 : 0 , sessionFactory);
		clearCounts(sessionFactory);

		scope.inTransaction(
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
					s.remove( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 3 , sessionFactory);
	}

	@Test
	public void testMoveOneToManyElementToNewEntityCollection(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		contract.addParty( new Party( "party" ) );

		scope.inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 , sessionFactory);
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		Contract contract2 = new Contract( null, "david", "phone" );

		scope.inTransaction(
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
					s.persist( contract2 );
				}
		);

		assertInsertCount( 1, sessionFactory );
		assertUpdateCount( isContractVersioned ? 1 : 0 , sessionFactory);
		clearCounts(sessionFactory);

		scope.inTransaction(
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
					s.remove( c );
					s.remove( c2 );
					assertPartyAndContractAreDeleted( s );

				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 3 , sessionFactory);
	}

	@Test
	public void testMoveOneToManyElementToExistingEntityCollection(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		contract.addParty( new Party( "party" ) );
		Contract contract2 = new Contract( null, "david", "phone" );

		scope.inTransaction(
				s -> {
					s.persist( contract );
					s.persist( contract2 );
				}
		);

		assertInsertCount( 3, sessionFactory );
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
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

		assertInsertCount( 0 , sessionFactory);
		assertUpdateCount( isContractVersioned ? 2 : 0, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
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
					s.remove( c );
					s.remove( c2 );
					assertPartyAndContractAreDeleted( s );

				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 3 , sessionFactory);
	}

	@Test
	public void testRemoveOneToManyElementUsingUpdate(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );
		contract.addParty( firstParty );

		scope.inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 , sessionFactory);
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		contract.removeParty( firstParty );
		assertEquals( 0, contract.getParties().size() );
		if ( isContractPartiesBidirectional ) {
			assertNull( firstParty.getContract() );
		}

		scope.inTransaction(
				s -> {
					s.merge( contract );
					s.merge( firstParty );
				}
		);

		if ( checkUpdateCountsAfterRemovingElementWithoutDelete() ) {
			assertUpdateCount( isContractVersioned && !isContractPartiesInverse ? 1 : 0, sessionFactory );
		}
		assertDeleteCount( 0 , sessionFactory);
		clearCounts(sessionFactory);

		scope.inTransaction(
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
						s.remove( party );
					}
					s.remove( c );
					assertPartyAndContractAreDeleted( s );

				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 2, sessionFactory );
	}

	@Test
	public void testRemoveOneToManyElementUsingMerge(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );
		contract.addParty( firstParty );

		scope.inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 , sessionFactory);
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		contract.removeParty( firstParty );
		assertEquals( 0, contract.getParties().size() );
		if ( isContractPartiesBidirectional ) {
			assertNull( firstParty.getContract() );
		}

		scope.inTransaction(
				s -> {
					s.merge( contract );
					s.merge( firstParty );
				}
		);


		if ( checkUpdateCountsAfterRemovingElementWithoutDelete() ) {
			assertUpdateCount( isContractVersioned && !isContractPartiesInverse ? 1 : 0, sessionFactory );
		}
		assertDeleteCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
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
						s.remove( party );
					}
					s.remove( c );
					assertPartyAndContractAreDeleted( s );
				}

		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 2, sessionFactory );
	}

	@Test
	public void testDeleteOneToManyElement(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );
		contract.addParty( firstParty );

		scope.inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 , sessionFactory);
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract merged = s.merge( contract );
					Iterator<Party> iterator = merged.getParties().iterator();
					while ( iterator.hasNext() ) {
						Party p = iterator.next();
						if ( p.getId() == firstParty.getId() ) {
							iterator.remove();
							s.remove( p );
						}
					}
				}
		);

		assertUpdateCount( isContractVersioned ? 1 : 0 , sessionFactory);
		assertDeleteCount( 1, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 0, c.getParties().size() );
					Party party = getParty( s );
					assertNull( party );
					s.remove( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 1, sessionFactory );
	}

	@Test
	public void testRemoveOneToManyElementByDelete(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		Party firstParty = new Party( "party" );
		contract.addParty( firstParty );

		scope.inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 , sessionFactory);
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		contract.removeParty( firstParty );
		assertEquals( 0, contract.getParties().size() );
		if ( isContractPartiesBidirectional ) {
			assertNull( firstParty.getContract() );
		}

		scope.inTransaction(
				s -> {
					Contract merged = s.merge( contract );
					s.remove( s.merge( firstParty ) );
				}
		);

		assertUpdateCount( isContractVersioned ? 1 : 0 , sessionFactory);
		assertDeleteCount( 1, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 0, c.getParties().size() );
					s.remove( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 1, sessionFactory );
	}

	@Test
	public void testRemoveOneToManyOrphanUsingUpdate(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		ContractVariation contractV = new ContractVariation( 1, contract );
		contractV.setText( "cv1" );

		scope.inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 , sessionFactory);
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		contract.getVariations().remove( contractV );
		contractV.setContract( null );
		assertEquals( 0, contract.getVariations().size() );
		if ( isContractVariationsBidirectional ) {
			assertNull( contractV.getContract() );
		}

		scope.inTransaction(
				s -> s.merge( contract )
		);

		assertUpdateCount( isContractVersioned ? 1 : 0 , sessionFactory);
		assertDeleteCount( 1, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 0, c.getVariations().size() );
					ContractVariation cv = getContractVariation( s );
					assertNull( cv );
					s.remove( c );
					assertPartyAndContractAreDeleted( s );

				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 1, sessionFactory );
	}

	@Test
	public void testRemoveOneToManyOrphanUsingMerge(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		clearCounts(sessionFactory);
		Contract contract = new Contract( null, "gail", "phone" );
		ContractVariation contractVariation = new ContractVariation( 1, contract );

		scope.inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 , sessionFactory);
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		contract.getVariations().remove( contractVariation );
		contractVariation.setContract( null );
		assertEquals( 0, contract.getVariations().size() );
		if ( isContractVariationsBidirectional ) {
			assertNull( contractVariation.getContract() );
		}

		scope.inTransaction(
				s -> {
					s.merge( contract );
					s.merge( contractVariation );
				}
		);

		assertUpdateCount( isContractVersioned ? 1 : 0 , sessionFactory);
		assertDeleteCount( 1, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 0, c.getVariations().size() );
					ContractVariation cv = getContractVariation( s );
					assertNull( cv );
					s.remove( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 1, sessionFactory );
	}

	@Test
	public void testDeleteOneToManyOrphan(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract contract = new Contract( null, "gail", "phone" );
		ContractVariation contractVariation = new ContractVariation( 1, contract );

		scope.inTransaction(
				s -> s.persist( contract )
		);

		assertInsertCount( 2 , sessionFactory);
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract merged = s.merge( contract );
					Iterator<ContractVariation> iterator = merged.getVariations().iterator();

					while ( iterator.hasNext() ) {
						ContractVariation cv = iterator.next();
						if ( cv.getId() == contractVariation.getId() ) {
							iterator.remove();
							cv.setContract( null );
							s.remove( cv );
						}
					}
				}
		);

		assertUpdateCount( isContractVersioned ? 1 : 0 , sessionFactory);
		assertDeleteCount( 1, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract c = getContract( s );
					assertEquals( 0, c.getVariations().size() );
					ContractVariation cv = getContractVariation( s );
					assertNull( cv );
					s.remove( c );
					assertPartyAndContractAreDeleted( s );
				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 1, sessionFactory );
	}

	@Test
	public void testOneToManyCollectionOptimisticLockingWithMerge(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		// Create a Contract with one Party
		final Contract originalContract = new Contract( null, "gail", "phone" );
		final Party originalParty = new Party( "party" );
		originalContract.addParty( originalParty );
		scope.inTransaction( (session) -> session.persist( originalContract ) );

		assertInsertCount( 2 , sessionFactory);
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		// Load the Contract created above and add a new Party
		//		- this should trigger a version increment, if `isContractVersioned`
		scope.inTransaction( (session) -> {
			final Contract loadedContract = session.get( Contract.class, originalContract.getId() );
			Party newParty = new Party( "new party" );
			loadedContract.addParty( newParty );
		} );

		assertInsertCount( 1, sessionFactory );
		assertUpdateCount( isContractVersioned ? 1 : 0 , sessionFactory);
		clearCounts(sessionFactory);

		// Using the now stale `originalContract` reference, remove the
		// first Party.  If `isContractVersioned`, this should trigger
		// a staleness exception
		scope.inSession( (session) -> {
			originalContract.removeParty( originalParty );
			try {
				session.merge( originalContract );
				assertFalse( isContractVersioned );
			}
			catch (PersistenceException ex) {
				assertTyping( StaleObjectStateException.class, ex.getCause() );
				assertTrue( isContractVersioned );
			}
			finally {
				session.getTransaction().rollback();
			}
		} );


		scope.inTransaction(
				s -> {
					Contract c = getContract( s );
					s.remove( c );
					assertPartyAndContractAreDeleted( s );

				}
		);

		assertUpdateCount( 0, sessionFactory );
		assertDeleteCount( 3 , sessionFactory);
	}

	@Test
	public void testOneToManyCollectionOptimisticLockingWithUpdate(SessionFactoryScope scope) {
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		clearCounts(sessionFactory);

		Contract cOrig = new Contract( null, "gail", "phone" );
		Party partyOrig = new Party( "party" );
		cOrig.addParty( partyOrig );
		scope.inTransaction(
				s -> s.persist( cOrig )
		);

		assertInsertCount( 2 , sessionFactory);
		assertUpdateCount( 0, sessionFactory );
		clearCounts(sessionFactory);

		scope.inTransaction(
				s -> {
					Contract c = s.get( Contract.class, cOrig.getId() );
					Party newParty = new Party( "new party" );
					c.addParty( newParty );
				}
		);

		assertInsertCount( 1, sessionFactory );
		assertUpdateCount( isContractVersioned ? 1 : 0 , sessionFactory);
		clearCounts(sessionFactory);

		scope.inSession(
				s -> {
					s.beginTransaction();
					cOrig.removeParty( partyOrig );
					try {
						s.merge( cOrig );
						s.getTransaction().commit();
						assertFalse( isContractVersioned );
					}
					catch (PersistenceException ex) {
						s.getTransaction().rollback();
						assertTrue( isContractVersioned );
						assertTyping( StaleObjectStateException.class, ex.getCause() );
					}
				}
		);

		scope.inTransaction(
				s -> {
					EntityGraph<Contract> entityGraph = s.createEntityGraph( Contract.class);
					Map<String, Object> properties = new HashMap();
					properties.put("javax.persistence.fetchgraph", entityGraph);
					Contract c = s.find( Contract.class, cOrig.getId(), properties );
					s.createMutationQuery( "delete from Party" ).executeUpdate();
					s.remove( c );
					s.flush();
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
		return s.createQuery( criteria )
				.setHint( HINT_JAVAEE_FETCH_GRAPH, s.createEntityGraph( Contract.class ) )
				.uniqueResult();
	}

	private ContractVariation getContractVariation(SessionImplementor s) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<ContractVariation> criteria = criteriaBuilder.createQuery( ContractVariation.class );
		criteria.from( ContractVariation.class );
		return s.createQuery( criteria )
				.setHint( HINT_JAVAEE_FETCH_GRAPH, s.createEntityGraph( ContractVariation.class ) )
				.uniqueResult();
	}

	private Party getParty(SessionImplementor s) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Party> criteria = criteriaBuilder.createQuery( Party.class );
		criteria.from( Party.class );
		return s.createQuery( criteria )
				.setHint( HINT_JAVAEE_FETCH_GRAPH, s.createEntityGraph( Party.class ) )
				.uniqueResult();
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

	protected void clearCounts(SessionFactoryImplementor sessionFactory) {
		sessionFactory.getStatistics().clear();
	}

	protected void assertInsertCount(int expected, SessionFactoryImplementor sessionFactory) {
		int inserts = (int) sessionFactory.getStatistics().getEntityInsertCount();
		assertEquals( expected, inserts, "unexpected insert count" );
	}

	protected void assertUpdateCount(int expected, SessionFactoryImplementor sessionFactory) {
		int updates = (int) sessionFactory.getStatistics().getEntityUpdateCount();
		assertEquals( expected, updates, "unexpected update counts" );
	}

	protected void assertDeleteCount(int expected, SessionFactoryImplementor sessionFactory) {
		int deletes = (int) sessionFactory.getStatistics().getEntityDeleteCount();
		assertEquals( expected, deletes, "unexpected delete counts" );
	}
}
