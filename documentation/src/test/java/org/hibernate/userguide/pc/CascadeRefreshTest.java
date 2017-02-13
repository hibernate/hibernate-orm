package org.hibernate.userguide.pc;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Fábio Takeo Ueno
 */
public class CascadeRefreshTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Phone.class
		};
	}

	@Test
	public void refreshTest() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.setId( 1L );
			person.setName( "John Doe" );

			Phone phone = new Phone();
			phone.setId( 1L );
			phone.setNumber( "123-456-7890" );

			person.addPhone( phone );
			entityManager.persist( person );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {

			//tag::pc-cascade-refresh-example[]
			Person person = entityManager.find( Person.class, 1L );
			Phone phone = person.getPhones().get( 0 );

			person.setName( "John Doe Jr." );
			phone.setNumber( "987-654-3210" );

			entityManager.refresh( person );

			assertEquals( "John Doe", person.getName() );
			assertEquals( "123-456-7890", phone.getNumber() );
			//end::pc-cascade-refresh-example[]
		} );
	}
}