package org.hibernate.internal;

import java.util.stream.Stream;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Dragoş Haiduc
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-14231" )
@RequiresDialectFeature( DialectChecks.SupportsExpectedLobUsagePattern.class )
public class ScrollableResultsObjectArrayCastingTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Product.class };
	}

	@Before
	public void SetUp() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Product product = new Product();
			product.binaryValue = new byte[] { 1, 2, 3 };
			entityManager.persist( product );
		} );
	}

	@Test
	public void testNoClassCastExceptionThrown() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			TypedQuery<byte[]> typedQuery = entityManager.createQuery( "select p.binaryValue from Product p", byte[].class );
			Stream<byte[]> stream = typedQuery.getResultStream();
			stream.findFirst();
		} );
	}

	@Entity(name = "Product")
	public static class Product {

		@Id @GeneratedValue
		Integer id;

		String name;

		@Lob
		byte[] binaryValue;
	}

}
