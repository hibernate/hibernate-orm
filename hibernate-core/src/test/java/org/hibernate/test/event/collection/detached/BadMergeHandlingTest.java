/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.event.collection.detached;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-7928" )
public class BadMergeHandlingTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Customer.class, Alias.class, CreditCard.class };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7928" )
	public void testMergeAndHold() {
		Session s = openSession();
		s.beginTransaction();
		Customer paul = new Customer( 1, "Paul Atreides" );
		s.persist( paul );

		Alias alias1 = new Alias( 1, "Paul Muad'Dib" );
		s.persist( alias1 );

		Alias alias2 = new Alias( 2, "Usul" );
		s.persist( alias2 );

		Alias alias3 = new Alias( 3, "The Preacher" );
		s.persist( alias3 );

		CreditCard cc1 = new CreditCard( 1 );
		s.persist( cc1 );

		CreditCard cc2 = new CreditCard( 2 );
		s.persist( cc2 );

		s.getTransaction().commit();
		s.close();

		// set up relationships
		s = openSession();
		s.beginTransaction();

		alias1.customers.add( paul );
		s.merge( alias1 );
		alias2.customers.add( paul );
		s.merge( alias2 );
		alias3.customers.add( paul );
		s.merge( alias3 );

		cc1.customer = paul;
		s.merge( cc1 );
		cc2.customer = paul;
		s.merge( cc2 );

		s.getTransaction().commit();
		s.close();

		// now try to read them back (I guess)
		s = openSession();
		s.beginTransaction();
		List results = s.createQuery( "select c from Customer c join c.aliases a where a.alias = :aParam" )
				.setParameter( "aParam", "Usul" )
				.list();
		assertEquals( 1, results.size() );
		s.getTransaction().commit();
		s.close();
	}

	@Entity( name="Customer" )
	public static class Customer {
		@Id
		private Integer id;
		private String name;
		@ManyToMany( cascade= CascadeType.ALL, mappedBy="customers" )
		private Collection<Alias> aliases = new ArrayList<Alias>();
		@OneToMany(cascade=CascadeType.ALL, mappedBy="customer")
		private Collection<CreditCard> creditCards = new ArrayList<CreditCard>();

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity( name="Alias" )
	public static class Alias {
		@Id
		private Integer id;
		private String alias;
		@ManyToMany(cascade=CascadeType.ALL)
		@JoinTable(name="FKS_ALIAS_CUSTOMER",
				   joinColumns=
				   @JoinColumn(
						   name="FK_FOR_ALIAS_TABLE", referencedColumnName="ID"),
				   inverseJoinColumns=
				   @JoinColumn(
						   name="FK_FOR_CUSTOMER_TABLE", referencedColumnName="ID")
		)
		private Collection<Customer> customers = new ArrayList<Customer>();

		public Alias() {
		}

		public Alias(Integer id, String alias) {
			this.id = id;
			this.alias = alias;
		}
	}

	@Entity( name="CreditCard" )
	public static class CreditCard {
		@Id
		private Integer id;
		@ManyToOne(cascade=CascadeType.ALL)
		@JoinColumn (name="FK3_FOR_CUSTOMER_TABLE")
		private Customer customer;

		public CreditCard() {
		}

		public CreditCard(Integer id) {
			this.id = id;
		}
	}
}
