/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.criteria.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.Address;
import org.hibernate.jpa.test.metamodel.Address_;
import org.hibernate.jpa.test.metamodel.Phone;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * Tests usage of {@link ListJoin#index()}
 *
 * @author Brett Meyer
 */
public class ListIndexTest extends AbstractMetamodelSpecificTest {
	
	@Test
	@TestForIssue(jiraKey = "HHH-8404")
	public void testListIndex() {
		EntityManager em = getOrCreateEntityManager();
		
		em.getTransaction().begin();
		
		Address address1 = new Address();
		address1.setId( "a1" );
		Phone phone1 = new Phone();
		phone1.setId( "p1" );
		phone1.setAddress( address1 );
		Phone phone2 = new Phone();
		phone2.setId( "p2" );
		
		phone2.setAddress( address1 );
		address1.getPhones().add( phone1 );
		address1.getPhones().add( phone2 );
		
		Address address2 = new Address();
		address2.setId( "a2" );
		Phone phone3 = new Phone();
		phone3.setId( "p3" );
		
		phone3.setAddress( address2 );
		address2.getPhones().add( phone3 );
		
		em.persist( phone1 );
		em.persist( phone2 );
		em.persist( phone3 );
		em.persist( address1 );
		em.persist( address2 );
		em.getTransaction().commit();
		em.clear();
		
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Address> criteria = cb.createQuery( Address.class );
		Root<Address> addressRoot = criteria.from( Address.class );
		ListJoin<Address, Phone> phones = addressRoot.join( Address_.phones );
		criteria.where( cb.gt( phones.index(), 0 ) );
		List<Address> results = em.createQuery( criteria ).getResultList();

		assertNotNull( results );
		// Ensure that the "index(phones) > 0" condition was included on the inner join, meaning only address1
		// (> 1 phone) was returned.
		assertEquals( 1, results.size() );
		assertEquals( address1.getId(), results.get( 0 ).getId() );
	}
}
