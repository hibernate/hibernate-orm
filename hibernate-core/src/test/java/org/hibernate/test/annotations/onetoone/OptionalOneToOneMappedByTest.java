/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 * @author Gail Badner
 */
public class OptionalOneToOneMappedByTest extends BaseCoreFunctionalTestCase {

	// @OneToOne(mappedBy="address") with foreign generator
	@Test
	public void testBidirForeignIdGenerator() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		OwnerAddress address = new OwnerAddress();
		address.setOwner( null );
		try {
			s.persist( address );
			s.flush();
			fail( "should have failed with IdentifierGenerationException" );
		}
		catch (IdentifierGenerationException ex) {
			// expected
		}
		finally {
			tx.rollback();
		}
		s.close();
	}

	@Test
	public void testBidirAssignedId() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		PartyAffiliate affiliate = new PartyAffiliate();
		affiliate.partyId = "id";

		s.persist( affiliate );
		s.getTransaction().commit();

		s.clear();

		Transaction tx = s.beginTransaction();

		affiliate = ( PartyAffiliate ) s.createCriteria(PartyAffiliate.class)
				.add( Restrictions.idEq( "id" ) )
				.uniqueResult();
		assertNotNull( affiliate );
		assertEquals( "id", affiliate.partyId );
		assertNull( affiliate.party );

		s.clear();

		affiliate = ( PartyAffiliate ) s.get( PartyAffiliate.class, "id" );
		assertNull( affiliate.party );

		s.delete( affiliate );
		tx.commit();
		s.close();
	}

	@Test
	public void testBidirDefaultIdGenerator() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		PersonAddress personAddress = new PersonAddress();
		personAddress.setPerson( null );

		s.persist( personAddress );
		s.getTransaction().commit();

		s.clear();

		Transaction tx = s.beginTransaction();

		personAddress = ( PersonAddress ) s.createCriteria(PersonAddress.class)
				.add( Restrictions.idEq( personAddress.getId() ) )
				.uniqueResult();
		assertNotNull( personAddress );
		assertNull( personAddress.getPerson() );

		s.clear();

		personAddress = ( PersonAddress ) s.get( PersonAddress.class, personAddress.getId() );
		assertNull( personAddress.getPerson() );

		s.delete( personAddress );
		tx.commit();
		s.close();
	}
        
        /**
         * <p>
         * Test case for HHH-5757
         * </p>
         * <p>
         * The SQL query :
         * <pre>
         * select this_.id as id1_4_1_, personaddr2_.id as id1_5_0_ from Person this_ left outer join PersonAddress personaddr2_ on this_.id=personaddr2_.id where personaddr2_.id=?
         * </pre>
         * is reduced by :
         * <pre>
         * select this_.id as id1_4_1_, personaddr2_.id as id1_5_0_ from Person this_ left outer join PersonAddress personaddr2_ on this_.id=personaddr2_.id where this_.id=?
         * </pre>
         * due to the OneToOne annotation.
         * </p>
         * <p>
         * It is not triggered by the optional specificity of the relation, but the fact person is optional should enforce the fact that the query
         * should not be reduced.
         * </p>
         * 
         * @throws Exception 
         */
        @Test(expected = GenericJDBCException.class)
        @TestForIssue(jiraKey = "HHH-5757")
	public void testBidirQueryEntityProperty() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		PersonAddress personAddress = new PersonAddress();
                Person person = new Person();
		personAddress.setPerson( person);
                person.setPersonAddress(personAddress);

                s.persist(person);
		s.persist( personAddress );
		s.getTransaction().commit();

		s.clear();

		Transaction tx = s.beginTransaction();

		personAddress = ( PersonAddress ) s.createCriteria(PersonAddress.class)
				.add( Restrictions.idEq( personAddress.getId() ) )
				.uniqueResult();
		assertNotNull( personAddress );
		assertNotNull( personAddress.getPerson() );

		s.clear();
                
                // this call throws GenericJDBCException
                personAddress = ( PersonAddress ) s.createCriteria(PersonAddress.class)
                                .add(Restrictions.eq("person", person))
                                .uniqueResult();
                
                // the other way should also work
                person = ( Person ) s.createCriteria(Person.class)
                                .add(Restrictions.eq("personAddress", person))
                                .uniqueResult();
                
		s.delete( personAddress );
		tx.commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Party.class,
				PartyAffiliate.class,
				Owner.class,
				OwnerAddress.class,
				Person.class,
				PersonAddress.class
		};
	}
}
