package org.hibernate.test.collection.bag;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests collection equals behavior given the semantic meaning of a bag
 *
 * @author Jasper Horn
 */
@TestForIssue(jiraKey = "HHH-5409")
public class PersistentBagEqualsTest {

	@Test
	public void testTwoEmptyBags() {
		PersistentBag bag1 = createBag();
		PersistentBag bag2 = createBag();

		assertEquals( bag1.equals( bag2 ), true );
	}

	@Test
	public void testTwoBagsWithDifferentContents() {
		PersistentBag bag1 = createBag();
		bag1.add( "item1" );

		PersistentBag bag2 = createBag();
		bag2.add( "item2" );

		System.out.println( "Added items" );

		assertEquals( bag1.equals( bag2 ), false );
	}

	@Test
	public void testTwoBagsWithSameContents() {
		PersistentBag bag1 = createBag();
		bag1.add( "item" );

		PersistentBag bag2 = createBag();
		bag2.add( "item" );

		assertEquals( bag1.equals( bag2 ), true );
	}

	@Test
	public void testBagWithSubsetOfOtherBag() {
		PersistentBag bag1 = createBag();
		bag1.add( "item1" );

		PersistentBag bag2 = createBag();
		bag2.add( "item1" );
		bag2.add( "item2" );

		assertEquals( bag1.equals( bag2 ), false );
	}

	@Test
	public void testBagWithSupersetOfOtherBag() {
		PersistentBag bag1 = createBag();
		bag1.add( "item1" );
		bag1.add( "item2" );

		PersistentBag bag2 = createBag();
		bag2.add( "item1" );

		assertEquals( bag1.equals( bag2 ), false );
	}

	@Test
	public void testTwoBagsWithItemsInDifferentOrder() {
		PersistentBag bag1 = createBag();
		bag1.add( "item1" );
		bag1.add( "item2" );

		PersistentBag bag2 = createBag();
		bag2.add( "item2" );
		bag2.add( "item1" );

		assertEquals( bag1.equals( bag2 ), true );
	}

	@Test
	public void testTwoBagsWithItemsInDifferentQuantities() {
		PersistentBag bag1 = createBag();
		bag1.add( "item1" );
		bag1.add( "item1" );

		PersistentBag bag2 = createBag();
		bag2.add( "item1" );

		assertEquals( bag1.equals( bag2 ), false );
	}

	@Test
	public void testTwoBagsWithItemsInSameQuantity() {
		PersistentBag bag1 = createBag();
		bag1.add( "item1" );
		bag1.add( "item1" );

		PersistentBag bag2 = createBag();
		bag2.add( "item1" );
		bag2.add( "item1" );

		assertEquals( bag1.equals( bag2 ), true );
	}

	private PersistentBag createBag() {
		return new PersistentBag( Mockito.mock( SharedSessionContractImplementor.class ), new ArrayList() );
	}
}
