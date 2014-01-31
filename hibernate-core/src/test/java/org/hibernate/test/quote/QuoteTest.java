/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeKeyBinding;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 * @author Brett Meyer
 */
public class QuoteTest extends BaseCoreFunctionalTestCase {
	
	@Test
	public void testQuoteManytoMany() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		User u = new User();
		s.persist( u );
		Role r = new Role();
		s.persist( r );
		u.getRoles().add( r );
		s.flush();
		s.clear();
		u = (User) s.get( User.class, u.getId() );
		assertEquals( 1, u.getRoles().size() );
		tx.rollback();
		String role = User.class.getName() + ".roles";
		AttributeBinding attributeBinding = metadata().getEntityBinding( User.class.getName() ).locateAttributeBinding( "roles" );
		PluralAttributeKeyBinding keyBinding = ( (PluralAttributeBinding) attributeBinding ).getPluralAttributeKeyBinding();
		assertEquals(
				"User_Role",
				( (org.hibernate.metamodel.spi.relational.Table) keyBinding.getCollectionTable() )
						.getPhysicalName().getText()
		);
		s.close();
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-8464")
	public void testDoubleQuoteJoinColumn() {
		Session s = openSession();
		s.getTransaction().begin();
		User user = new User();
		House house = new House();
		user.setHouse( house );
		s.persist( house );
		s.persist( user );
		s.getTransaction().commit();
		s.clear();
		
		s = openSession();
		s.getTransaction().begin();
		user = (User) s.get( User.class, user.getId() );
		assertNotNull( user );
		assertNotNull( user.getHouse() );
		// seems trivial, but if quoting normalization worked on the join column, these should all be the same
		assertEquals( user.getHouse().getId(), user.getHouse1() );
		assertEquals( user.getHouse().getId(), user.getHouse2() );
		s.getTransaction().commit();
		s.close();
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-2988")
	public void testUnionSubclassEntityQuoting() {
		Session s = openSession();
		s.beginTransaction();
		Container container1 = new Container();
		Container container2 = new Container();
		SimpleItem simpleItem = new SimpleItem();
		
		container1.items.add( container2 );
		container1.items.add( simpleItem );
		container2.parent = container1;
		simpleItem.parent = container1;
		
		s.persist( simpleItem );
		s.persist( container2 );
		s.persist( container1 );
		s.getTransaction().commit();
		s.clear();
		
		Container result = (Container) s.get( Container.class, container1.id );
		assertNotNull( result );
		assertNotNull( result.items );
		assertEquals( 2, result.items.size() );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				User.class,
				Role.class,
				Phone.class,
				House.class,
				Container.class,
				SimpleItem.class
		};
	}
	
	@Entity
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	private static abstract class Item {

		@Id @GeneratedValue
		@Column(name = "`ID`")
		protected long id;
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "`ParentID`")
		protected Container parent;
	}

	@Entity
	@Table(name = "`CoNTaiNeR`")
	private static class Container extends Item {

		@OneToMany(mappedBy = "parent", targetEntity = Item.class)
		private Set<Item> items = new HashSet<Item>( 0 );
	}

	@Entity
	@Table(name = "`SimpleItem`")
	private static class SimpleItem extends Item {
	}
}
