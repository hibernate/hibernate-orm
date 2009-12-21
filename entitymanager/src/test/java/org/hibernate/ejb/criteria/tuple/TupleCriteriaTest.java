package org.hibernate.ejb.criteria.tuple;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class TupleCriteriaTest extends TestCase {
	public void testArray() {
		EntityManager em = factory.createEntityManager();
		Customer c1 = new Customer();
		c1.setAge( 18 );
		c1.setName( "Bob" );
		em.getTransaction().begin();
		em.persist( c1 );
		em.flush();

		final CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Object[]> q = cb.createQuery(Object[].class);
		Root<Customer> c = q.from(Customer.class);
		q.select( cb.array( c.get(Customer_.name), c.get(Customer_.age) ) );
		List<Object[]> result = em.createQuery(q).getResultList();

		assertEquals( 1, result.size() );
		assertEquals( c1.getName(), result.get( 0 )[0] );
		assertEquals( c1.getAge(), result.get( 0 )[1] );

		em.getTransaction().rollback();
		em.close();
	}


	public void testTuple() {
		EntityManager em = factory.createEntityManager();
		Customer c1 = new Customer();
		c1.setAge( 18 );
		c1.setName( "Bob" );
		em.getTransaction().begin();
		em.persist( c1 );
		em.flush();

		final CriteriaBuilder cb = em.getCriteriaBuilder();

		CriteriaQuery<Tuple> q = cb.createTupleQuery();
		Root<Customer> c = q.from(Customer.class);
		Path<String> tname = c.get(Customer_.name);
		q.multiselect(  tname, c.get(Customer_.age).alias("age") );
		List<Tuple> result = em.createQuery(q).getResultList();

		assertEquals( 1, result.size() );
		//FIXME uncomment when HHH-4724 is fixed
//		assertEquals( c1.getName(), result.get(0).get(tname) );
//		assertEquals( c1.getAge(), result.get(0).get("age") );
				

		em.getTransaction().rollback();
		em.close();
	}

	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Customer.class
		};
	}
}
