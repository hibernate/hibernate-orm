package org.hibernate.ejb.criteria.tuple;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;

import org.hibernate.ejb.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.ejb.metamodel.Customer;
import org.hibernate.ejb.metamodel.Customer_;

/**
 * @author Emmanuel Bernard
 */
public class TupleCriteriaTest extends AbstractMetamodelSpecificTest {
	public void testArray() {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		Customer c1 = new Customer();
		c1.setId( "c1" );
		c1.setAge( 18 );
		c1.setName( "Bob" );
		em.persist( c1 );
		em.getTransaction().commit();
		em.close();

		em = factory.createEntityManager();
		em.getTransaction().begin();
		final CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Object[]> q = cb.createQuery(Object[].class);
		Root<Customer> c = q.from(Customer.class);
		q.select( cb.array( c.get(Customer_.name), c.get(Customer_.age) ) );
		List<Object[]> result = em.createQuery(q).getResultList();
		assertEquals( 1, result.size() );
		assertEquals( c1.getName(), result.get( 0 )[0] );
		assertEquals( c1.getAge(), result.get( 0 )[1] );
		em.getTransaction().commit();
		em.close();

		em = factory.createEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Customer" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}


	public void testTuple() {
		EntityManager em = factory.createEntityManager();

		em.getTransaction().begin();
		Customer c1 = new Customer();
		c1.setId( "c1" );
		c1.setAge( 18 );
		c1.setName( "Bob" );
		em.persist( c1 );
		em.getTransaction().commit();
		em.close();

		em = factory.createEntityManager();
		em.getTransaction().begin();
		final CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Tuple> criteria = builder.createTupleQuery();
		Root<Customer> customerRoot = criteria.from( Customer.class );
		Path<String> namePath = customerRoot.get( Customer_.name );
		Path<Integer> agePath = customerRoot.get( Customer_.age );
		agePath.alias( "age" );
		criteria.multiselect( namePath, agePath );
		List<Tuple> results = em.createQuery( criteria ).getResultList();
		assertEquals( 1, results.size() );
		Object resultElement = results.get( 0 );
		assertTrue( "Check  result 'row' as Tuple", Tuple.class.isInstance( resultElement ) );
		Tuple resultElementTuple = (Tuple) resultElement;
		Object[] tupleArray = resultElementTuple.toArray();
		assertEquals( 2, tupleArray.length );
		assertEquals( tupleArray[0], resultElementTuple.get( 0 ) );
		assertEquals( resultElementTuple.get( namePath ), resultElementTuple.get( 0 ) );
		assertEquals( tupleArray[1], resultElementTuple.get( 1 ) );
		assertEquals( resultElementTuple.get( agePath ), resultElementTuple.get( 1 ) );
		assertEquals( resultElementTuple.get( agePath ), resultElementTuple.get( "age" ) );
		em.getTransaction().commit();
		em.close();

		em = factory.createEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Customer" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

}
