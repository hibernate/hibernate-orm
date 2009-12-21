package org.hibernate.ejb.test.util;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class GetIdentifierTest extends TestCase {

	public void testSimpleId() {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		Book book = new Book();
		em.persist( book );
		em.flush();
		assertEquals( book.getId(), em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( book ) );
		em.getTransaction().rollback();
		em.close();
	}

	public void testEmbeddedId() {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		Umbrella umbrella = new Umbrella();
		umbrella.setId( new Umbrella.PK() );
		umbrella.getId().setBrand( "Burberry" );
		umbrella.getId().setModel( "Red Hat" );
		em.persist( umbrella );
		em.flush();
		assertEquals( umbrella.getId(), em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( umbrella ) );
		em.getTransaction().rollback();
		em.close();
	}

	public void testIdClass() {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		Sickness sick = new Sickness();

		sick.setClassification( "H1N1" );
		sick.setType("Flu");
		em.persist( sick );
		em.flush();
		Sickness.PK id = new Sickness.PK();
		id.setClassification( sick.getClassification() );
		id.setType( sick.getType() );
		assertEquals( id, em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( sick ) );
		em.getTransaction().rollback();
		em.close();
	}

	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Umbrella.class,
				Sickness.class
		};
	}
}
