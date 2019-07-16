/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.immutable.entitywithmutablecollection;

import java.util.Iterator;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.MappingException;
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
public abstract class AbstractEntityWithManyToManyTest extends BaseCoreFunctionalTestCase {
	private boolean isPlanContractsInverse;
	private boolean isPlanContractsBidirectional;
	private boolean isPlanVersioned;
	private boolean isContractVersioned;

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void prepareTest() throws Exception {
		super.prepareTest();
		isPlanContractsInverse = sessionFactory().getCollectionPersister( Plan.class.getName() + ".contracts" )
				.isInverse();
		try {
			sessionFactory().getCollectionPersister( Contract.class.getName() + ".plans" );
			isPlanContractsBidirectional = true;
		}
		catch (MappingException ex) {
			isPlanContractsBidirectional = false;
		}
		isPlanVersioned = sessionFactory().getEntityPersister( Plan.class.getName() ).isVersioned();
		isContractVersioned = sessionFactory().getEntityPersister( Contract.class.getName() ).isVersioned();
		sessionFactory().getStatistics().clear();
	}

	@Test
	public void testUpdateProperty() {
		clearCounts();

		inTransaction(
				s -> {
					Plan p = new Plan( "plan" );
					p.addContract( new Contract( null, "gail", "phone" ) );
					s.persist( p );
				}
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p = getPlan( s );
					p.setDescription( "new plan" );
					assertEquals( 1, p.getContracts().size() );
					Contract c = (Contract) p.getContracts().iterator().next();
					c.setCustomerName( "yogi" );
				}
		);

		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p = getPlan( s );
					assertEquals( 1, p.getContracts().size() );
					Contract c = (Contract) p.getContracts().iterator().next();
					assertEquals( "gail", c.getCustomerName() );
					if ( isPlanContractsBidirectional ) {
						assertEquals( 1, c.getPlans().size() );
						assertSame( p, c.getPlans().iterator().next() );
					}
					s.delete( p );

					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testCreateWithNonEmptyManyToManyCollectionOfNew() {
		clearCounts();

		inTransaction(
				s -> {
					Plan p = new Plan( "plan" );
					p.addContract( new Contract( null, "gail", "phone" ) );
					s.persist( p );
				}
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p = getPlan( s );
					assertEquals( 1, p.getContracts().size() );
					Contract c = (Contract) p.getContracts().iterator().next();
					assertEquals( "gail", c.getCustomerName() );
					if ( isPlanContractsBidirectional ) {
						assertEquals( 1, c.getPlans().size() );
						assertSame( p, c.getPlans().iterator().next() );
					}
					s.delete( p );

					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testCreateWithNonEmptyManyToManyCollectionOfExisting() {
		clearCounts();

		Contract c = new Contract( null, "gail", "phone" );
		inTransaction(
				s -> s.persist( c )
		);

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p = new Plan( "plan" );
					p.addContract( c );
					s.save( p );
				}
		);

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p1 = getPlan( s );
					assertEquals( 1, p1.getContracts().size() );
					Contract c1 = (Contract) p1.getContracts().iterator().next();
					assertEquals( "gail", c1.getCustomerName() );
					if ( isPlanContractsBidirectional ) {
						assertEquals( 1, c1.getPlans().size() );
						assertSame( p1, c1.getPlans().iterator().next() );
					}
					s.delete( p1 );
					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testAddNewManyToManyElementToPersistentEntity() {
		clearCounts();

		Plan p = new Plan( "plan" );
		inTransaction(
				s -> {
					s.persist( p );
				}
		);

		assertInsertCount( 1 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p1 = s.get( Plan.class, p.getId() );
					assertEquals( 0, p.getContracts().size() );
					p1.addContract( new Contract( null, "gail", "phone" ) );
				}
		);

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p1 = getPlan( s );
					assertEquals( 1, p1.getContracts().size() );
					Contract c = (Contract) p1.getContracts().iterator().next();
					assertEquals( "gail", c.getCustomerName() );
					if ( isPlanContractsBidirectional ) {
						assertEquals( 1, c.getPlans().size() );
						assertSame( p1, c.getPlans().iterator().next() );
					}
					s.delete( p1 );
					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testAddExistingManyToManyElementToPersistentEntity() {
		clearCounts();

		Plan p = new Plan( "plan" );
		Contract c = new Contract( null, "gail", "phone" );
		inTransaction(
				s -> {
					s.persist( p );
					s.persist( c );
				}
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p1 = s.get( Plan.class, p.getId() );
					assertEquals( 0, p1.getContracts().size() );
					Contract c1 = s.get( Contract.class, c.getId() );
					if ( isPlanContractsBidirectional ) {
						assertEquals( 0, c1.getPlans().size() );
					}
					p1.addContract( c1 );
				}
		);

		assertInsertCount( 0 );
		assertUpdateCount( isContractVersioned && isPlanVersioned ? 2 : 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p1 = getPlan( s );
					assertEquals( 1, p1.getContracts().size() );
					Contract c1 = (Contract) p1.getContracts().iterator().next();
					assertEquals( "gail", c1.getCustomerName() );
					if ( isPlanContractsBidirectional ) {
						assertSame( p1, c1.getPlans().iterator().next() );
					}
					s.delete( p1 );
					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testCreateWithEmptyManyToManyCollectionUpdateWithExistingElement() {
		clearCounts();

		Plan p = new Plan( "plan" );
		Contract c = new Contract( null, "gail", "phone" );
		inTransaction(
				s -> {
					s.persist( p );
					s.persist( c );
				}
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		p.addContract( c );

		inTransaction(
				s -> s.update( p )
		);

		assertInsertCount( 0 );
		assertUpdateCount( isContractVersioned && isPlanVersioned ? 2 : 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p1 = getPlan( s );
					assertEquals( 1, p1.getContracts().size() );
					Contract c1 = (Contract) p1.getContracts().iterator().next();
					assertEquals( "gail", c1.getCustomerName() );
					if ( isPlanContractsBidirectional ) {
						assertSame( p1, c1.getPlans().iterator().next() );
					}
					s.delete( p1 );
					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testCreateWithNonEmptyManyToManyCollectionUpdateWithNewElement() {
		clearCounts();

		Plan p = new Plan( "plan" );
		Contract c = new Contract( null, "gail", "phone" );
		p.addContract( c );
		inTransaction(
				s -> s.persist( p )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		Contract newC = new Contract( null, "sherman", "telepathy" );
		p.addContract( newC );

		inTransaction(
				s -> s.update( p )
		);

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p1 = getPlan( s );
					assertEquals( 2, p1.getContracts().size() );
					for ( Iterator it = p1.getContracts().iterator(); it.hasNext(); ) {
						Contract aContract = (Contract) it.next();
						if ( aContract.getId() == c.getId() ) {
							assertEquals( "gail", aContract.getCustomerName() );
						}
						else if ( aContract.getId() == newC.getId() ) {
							assertEquals( "sherman", aContract.getCustomerName() );
						}
						else {
							fail( "unknown contract" );
						}
						if ( isPlanContractsBidirectional ) {
							assertSame( p1, aContract.getPlans().iterator().next() );
						}
					}
					s.delete( p1 );
					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testCreateWithEmptyManyToManyCollectionMergeWithExistingElement() {
		clearCounts();

		Plan p = new Plan( "plan" );
		Contract c = new Contract( null, "gail", "phone" );

		inTransaction(
				s -> {
					s.persist( p );
					s.persist( c );
				}
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		p.addContract( c );

		inTransaction(
				s -> s.merge( p )
		);

		assertInsertCount( 0 );
		assertUpdateCount( isContractVersioned && isPlanVersioned ? 2 : 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p1 = getPlan( s );
					assertEquals( 1, p1.getContracts().size() );
					Contract c1 = (Contract) p1.getContracts().iterator().next();
					assertEquals( "gail", c1.getCustomerName() );
					if ( isPlanContractsBidirectional ) {
						assertSame( p1, c1.getPlans().iterator().next() );
					}
					s.delete( p1 );
					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testCreateWithNonEmptyManyToManyCollectionMergeWithNewElement() {
		clearCounts();

		Plan p = new Plan( "plan" );
		Contract c = new Contract( null, "gail", "phone" );
		p.addContract( c );
		inTransaction(
				s -> s.persist( p )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		Contract newC = new Contract( null, "yogi", "mail" );
		p.addContract( newC );

		inTransaction(
				s -> s.merge( p )
		);

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned && isPlanVersioned ? 2 : 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p1 = getPlan( s );
					assertEquals( 2, p1.getContracts().size() );
					for ( Object o : p1.getContracts() ) {
						Contract aContract = (Contract) o;
						if ( aContract.getId() == c.getId() ) {
							assertEquals( "gail", aContract.getCustomerName() );
						}
						else if ( !aContract.getCustomerName().equals( newC.getCustomerName() ) ) {
							fail( "unknown contract:" + aContract.getCustomerName() );
						}
						if ( isPlanContractsBidirectional ) {
							assertSame( p1, aContract.getPlans().iterator().next() );
						}
					}
					s.delete( p1 );
					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testRemoveManyToManyElementUsingUpdate() {
		clearCounts();

		Plan p = new Plan( "plan" );
		Contract c = new Contract( null, "gail", "phone" );
		p.addContract( c );

		inTransaction(
				s -> s.persist( p )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		p.removeContract( c );
		assertEquals( 0, p.getContracts().size() );
		if ( isPlanContractsBidirectional ) {
			assertEquals( 0, c.getPlans().size() );
		}
		inTransaction(
				s -> s.update( p )
		);

		assertUpdateCount( isContractVersioned ? 1 : 0 );
		assertDeleteCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p1 = getPlan( s );
					if ( isPlanContractsInverse ) {
						assertEquals( 1, p1.getContracts().size() );
						Contract c1 = (Contract) p1.getContracts().iterator().next();
						assertEquals( "gail", c1.getCustomerName() );
						assertSame( p1, c1.getPlans().iterator().next() );
					}
					else {
						assertEquals( 0, p1.getContracts().size() );
						Contract c1 = getContract( s );
						if ( isPlanContractsBidirectional ) {
							assertEquals( 0, c1.getPlans().size() );
						}
						s.delete( c1 );
					}
					s.delete( p );
					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testRemoveManyToManyElementUsingUpdateBothSides() {
		clearCounts();

		Plan p = new Plan( "plan" );
		Contract c = new Contract( null, "gail", "phone" );
		p.addContract( c );

		inTransaction(
				s -> s.persist( p )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		p.removeContract( c );
		assertEquals( 0, p.getContracts().size() );
		if ( isPlanContractsBidirectional ) {
			assertEquals( 0, c.getPlans().size() );
		}
		inTransaction(
				s -> {
					s.update( p );
					s.update( c );
				}
		);

		assertUpdateCount( isContractVersioned && isPlanVersioned ? 2 : 0 );
		assertDeleteCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p1 = getPlan( s );
					assertEquals( 0, p1.getContracts().size() );
					Contract c1 = getContract( s );
					if ( isPlanContractsBidirectional ) {
						assertEquals( 0, c1.getPlans().size() );
					}
					s.delete( c1 );
					s.delete( p1 );
					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testRemoveManyToManyElementUsingMerge() {
		clearCounts();

		Plan p = new Plan( "plan" );
		Contract c = new Contract( null, "gail", "phone" );
		p.addContract( c );

		inTransaction(
				s -> s.persist( p )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		p.removeContract( c );
		assertEquals( 0, p.getContracts().size() );
		if ( isPlanContractsBidirectional ) {
			assertEquals( 0, c.getPlans().size() );
		}

		inTransaction(
				s -> s.merge( p )
		);

		assertUpdateCount( isContractVersioned ? 1 : 0 );
		assertDeleteCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p1 = getPlan( s );
					if ( isPlanContractsInverse ) {
						assertEquals( 1, p1.getContracts().size() );
						Contract c1 = (Contract) p1.getContracts().iterator().next();
						assertEquals( "gail", c1.getCustomerName() );
						assertSame( p1, c1.getPlans().iterator().next() );
					}
					else {
						assertEquals( 0, p1.getContracts().size() );
						Contract c1 = (Contract) getContract( s );
						if ( isPlanContractsBidirectional ) {
							assertEquals( 0, c1.getPlans().size() );
						}
						s.delete( c1 );
					}
					s.delete( p1 );
					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testRemoveManyToManyElementUsingMergeBothSides() {
		clearCounts();

		Plan p = new Plan( "plan" );
		Contract c = new Contract( null, "gail", "phone" );
		p.addContract( c );

		inTransaction(
				s -> s.persist( p )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		p.removeContract( c );
		assertEquals( 0, p.getContracts().size() );
		if ( isPlanContractsBidirectional ) {
			assertEquals( 0, c.getPlans().size() );
		}

		inTransaction(
				s -> {
					s.merge( p );
					s.merge( c );
				}
		);

		assertUpdateCount( isContractVersioned && isPlanVersioned ? 2 : 0 );
		assertDeleteCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p1 = getPlan( s );
					assertEquals( 0, p1.getContracts().size() );
					Contract c1 = getContract( s );
					if ( isPlanContractsBidirectional ) {
						assertEquals( 0, c1.getPlans().size() );
					}
					s.delete( c1 );
					s.delete( p1 );
					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 2 );
	}

	@Test
	public void testDeleteManyToManyElement() {
		clearCounts();

		Plan p = new Plan( "plan" );
		Contract c = new Contract( null, "gail", "phone" );
		p.addContract( c );

		inTransaction(
				s -> s.persist( p )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					s.update( p );
					p.removeContract( c );
					s.delete( c );
				}
		);

		assertUpdateCount( isContractVersioned ? 1 : 0 );
		assertDeleteCount( 1 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p1 = getPlan( s );
					assertEquals( 0, p1.getContracts().size() );
					Contract c1 = getContract( s );
					assertNull( c1 );
					s.delete( p1 );
					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
	}

	@Test
	public void testRemoveManyToManyElementByDelete() {
		clearCounts();

		Plan p = new Plan( "plan" );
		Contract c = new Contract( null, "gail", "phone" );
		p.addContract( c );

		inTransaction(
				s -> s.persist( p )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		p.removeContract( c );
		assertEquals( 0, p.getContracts().size() );
		if ( isPlanContractsBidirectional ) {
			assertEquals( 0, c.getPlans().size() );
		}

		inTransaction(
				s -> {
					s.update( p );
					s.delete( c );
				}
		);

		assertUpdateCount( isPlanVersioned ? 1 : 0 );
		assertDeleteCount( 1 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p1 = getPlan( s );
					assertEquals( 0, p1.getContracts().size() );
					s.delete( p1 );
					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 1 );
	}

	@Test
	public void testManyToManyCollectionOptimisticLockingWithMerge() {
		clearCounts();

		Plan pOrig = new Plan( "plan" );
		Contract cOrig = new Contract( null, "gail", "phone" );
		pOrig.addContract( cOrig );
		inTransaction(
				s -> s.persist( pOrig )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p = s.get( Plan.class, pOrig.getId() );
					Contract newC = new Contract( null, "sherman", "note" );
					p.addContract( newC );
				}
		);


		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		inSession(
				s -> {
					pOrig.removeContract( cOrig );
					try {
						s.merge( pOrig );
						assertFalse( isContractVersioned );
					}
					catch (PersistenceException ex) {
						assertTyping(StaleObjectStateException.class, ex.getCause());
						assertTrue( isContractVersioned);
					}
					finally {
						s.getTransaction().rollback();
					}
				}
		);



		inTransaction(
				s -> {
					Plan p1 = getPlan( s );
					s.delete( p1 );
					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testManyToManyCollectionOptimisticLockingWithUpdate() {
		clearCounts();

		Plan pOrig = new Plan( "plan" );
		Contract cOrig = new Contract( null, "gail", "phone" );
		pOrig.addContract( cOrig );
		inTransaction(
				s -> s.persist( pOrig )
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p = s.get( Plan.class, pOrig.getId() );
					Contract newC = new Contract( null, "yogi", "pawprint" );
					p.addContract( newC );
				}
		);

		assertInsertCount( 1 );
		assertUpdateCount( isContractVersioned ? 1 : 0 );
		clearCounts();

		inSession(
				s -> {
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
					Plan p1 = getPlan( s );
					s.delete( p1 );
					s.createQuery( "delete from Contract" ).executeUpdate();
					assertAllPlansAndContractsAreDeleted( s );
				}
		);
	}

	@Test
	public void testMoveManyToManyElementToNewEntityCollection() {
		clearCounts();

		Plan p = new Plan( "plan" );
		inTransaction(
				s -> {
					p.addContract( new Contract( null, "gail", "phone" ) );
					s.persist( p );
				}
		);

		assertInsertCount( 2 );
		assertUpdateCount( 0 );
		clearCounts();

		Plan p2 = new Plan( "new plan" );
		inTransaction(
				s -> {
					Plan p1 = getPlan( s );
					assertEquals( 1, p1.getContracts().size() );
					Contract c = (Contract) p1.getContracts().iterator().next();
					assertEquals( "gail", c.getCustomerName() );
					if ( isPlanContractsBidirectional ) {
						assertSame( p1, c.getPlans().iterator().next() );
					}
					p.removeContract( c );

					p2.addContract( c );
					s.save( p2 );
				}
		);

		assertInsertCount( 1 );
		assertUpdateCount( isPlanVersioned && isContractVersioned ? 2 : 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p1 = getPlan( s, p.getId() );
					Plan p3 = getPlan( s, p2.getId() );
		/*
		if ( isPlanContractsInverse ) {
			assertEquals( 1, p1.getContracts().size() );
			c = ( Contract ) p1.getContracts().iterator().next();
			assertEquals( "gail", c.getCustomerName() );
			if ( isPlanContractsBidirectional ) {
				assertSame( p1, c.getPlans().iterator().next() );
			}
			assertEquals( 0, p2.getContracts().size() );
		}
		else {
		*/
					assertEquals( 0, p1.getContracts().size() );
					assertEquals( 1, p3.getContracts().size() );
					Contract c = (Contract) p3.getContracts().iterator().next();
					assertEquals( "gail", c.getCustomerName() );
					if ( isPlanContractsBidirectional ) {
						assertSame( p3, c.getPlans().iterator().next() );
					}
					//}
					s.delete( p1 );
					s.delete( p3 );
					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	@Test
	public void testMoveManyToManyElementToExistingEntityCollection() {
		clearCounts();

		Plan p = new Plan( "plan" );
		Contract contract = new Contract( null, "gail", "phone" );
		p.addContract( contract );
		Plan p2 = new Plan( "plan2" );
		inTransaction(
				s -> {
					s.persist( p );
					s.persist( p2 );
				}
		);

		assertInsertCount( 3 );
		assertUpdateCount( 0 );
		clearCounts();


		inTransaction(
				s -> {
					Plan p3 = getPlan( s, p.getId() );
					assertEquals( 1, p3.getContracts().size() );
					Contract c = (Contract) p3.getContracts().iterator().next();
					assertEquals( "gail", c.getCustomerName() );
					if ( isPlanContractsBidirectional ) {
						assertSame( p3, c.getPlans().iterator().next() );
					}
					p3.removeContract( c );
				}
		);

		assertInsertCount( 0 );
		assertUpdateCount( isPlanVersioned && isContractVersioned ? 2 : 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p3 = getPlan( s, p2.getId() );
					Contract c1 = getContract( s, contract.getId() );
					p3.addContract( c1 );
				}
		);

		assertInsertCount( 0 );
		assertUpdateCount( isPlanVersioned && isContractVersioned ? 2 : 0 );
		clearCounts();

		inTransaction(
				s -> {
					Plan p3 = getPlan( s, p.getId() );
					Plan p4 = getPlan( s, p2.getId() );
		/*
		if ( isPlanContractsInverse ) {
			assertEquals( 1, p3.getContracts().size() );
			c = ( Contract ) p3.getContracts().iterator().next();
			assertEquals( "gail", c.getCustomerName() );
			if ( isPlanContractsBidirectional ) {
				assertSame( p3, c.getPlans().iterator().next() );
			}
			assertEquals( 0, p4.getContracts().size() );
		}
		else {
		*/
					assertEquals( 0, p3.getContracts().size() );
					assertEquals( 1, p4.getContracts().size() );
					Contract c1 = (Contract) p4.getContracts().iterator().next();
					assertEquals( "gail", c1.getCustomerName() );
					if ( isPlanContractsBidirectional ) {
						assertSame( p3, c1.getPlans().iterator().next() );
					}
					//}
					s.delete( p3 );
					s.delete( p4 );
					assertAllPlansAndContractsAreDeleted( s );
				}
		);

		assertUpdateCount( 0 );
		assertDeleteCount( 3 );
	}

	private void assertAllPlansAndContractsAreDeleted(SessionImplementor s) {
		assertEquals( new Long( 0 ), getPlanRowCount( s ) );
		assertEquals( new Long( 0 ), getContractRowCount( s ) );
	}

	private Plan getPlan(SessionImplementor s) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Plan> criteria = criteriaBuilder.createQuery( Plan.class );
		criteria.from( Plan.class );
		return s.createQuery( criteria ).uniqueResult();
	}

	private Plan getPlan(SessionImplementor s, long id) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Plan> criteria = criteriaBuilder.createQuery( Plan.class );
		Root<Plan> root = criteria.from( Plan.class );
		criteria.where( criteriaBuilder.equal( root.get( "id" ), id ) );
		return s.createQuery( criteria ).uniqueResult();
	}

	private Contract getContract(SessionImplementor s) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Contract> criteria = criteriaBuilder.createQuery( Contract.class );
		criteria.from( Contract.class );
		return s.createQuery( criteria ).uniqueResult();
	}

	private Contract getContract(SessionImplementor s, long id) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Contract> criteria = criteriaBuilder.createQuery( Contract.class );
		Root<Contract> root = criteria.from( Contract.class );
		criteria.where( criteriaBuilder.equal( root.get( "id" ), id ) );
		return s.createQuery( criteria ).uniqueResult();
	}

	private Long getPlanRowCount(SessionImplementor s) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Long> criteriaPlanRowCount = criteriaBuilder.createQuery( Long.class );
		criteriaPlanRowCount.select( criteriaBuilder.count( criteriaPlanRowCount.from( Plan.class ) ) );
		return s.createQuery( criteriaPlanRowCount ).uniqueResult();
	}

	private Long getContractRowCount(SessionImplementor s) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Long> criteriaContractRowCount = criteriaBuilder.createQuery( Long.class );
		criteriaContractRowCount.select( criteriaBuilder.count( criteriaContractRowCount.from( Contract.class ) ) );
		return s.createQuery( criteriaContractRowCount ).uniqueResult();
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
