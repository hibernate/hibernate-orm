package org.hibernate.query;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Andrias Sundskar
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-14213" )
public class IntegerRepresentationLiteralParsingExceptionTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ExampleEntity.class };
	}

	@Test
	public void testAppropriateExceptionMessageGenerated() {
		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				// -9223372036854775808 is beyond Long range, so an Exception will be thrown
				entityManager.createQuery( "select count(*) from ExampleEntity where counter = -9223372036854775808L" )
						.getSingleResult();
			} );
			Assert.fail( "Exception should be thrown" );
		}
		catch (Exception e) {
			// without fixing HHH-14213, the following exception would be thrown:
			// "Could not parse literal [9223372036854775808L] as integer"
			// which is confusing and misleading
			Assert.assertTrue( e.getMessage().endsWith( " as java.lang.Long" ) );
		}
	}

	@Entity(name = "ExampleEntity")
	static class ExampleEntity {

		@Id
		int id;

		long counter;
	}
}
