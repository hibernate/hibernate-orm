package org.hibernate.userguide.pc;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Fábio Takeo Ueno
 */
public class CascadeLockTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Phone.class
		};
	}

	@Test
	public void lockTest() {
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

			//tag::pc-cascade-lock-example[]
			Person person = entityManager.find( Person.class, 1L );
			assertEquals( 1, person.getPhones().size() );
			Phone phone = person.getPhones().get( 0 );

			assertTrue( entityManager.contains( person ) );
			assertTrue( entityManager.contains( phone ) );

			entityManager.detach( person );

			assertFalse( entityManager.contains( person ) );
			assertFalse( entityManager.contains( phone ) );

			entityManager.unwrap( Session.class )
					.buildLockRequest( new LockOptions( LockMode.NONE ) )
					.lock( person );

			assertTrue( entityManager.contains( person ) );
			assertTrue( entityManager.contains( phone ) );
			//end::pc-cascade-lock-example[]
		} );
	}
}