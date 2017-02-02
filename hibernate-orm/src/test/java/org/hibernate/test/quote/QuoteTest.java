/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.quote;

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

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 * @author Brett Meyer
 */
public class QuoteTest extends BaseNonConfigCoreFunctionalTestCase {
	
	@Test
	public void testQuoteManytoMany() {
		String role = User.class.getName() + ".roles";
		assertEquals( "User_Role", metadata().getCollectionBinding( role ).getCollectionTable().getName() );

		Session s = openSession();
		s.beginTransaction();
		User u = new User();
		s.persist( u );
		Role r = new Role();
		s.persist( r );
		u.getRoles().add( r );
		s.flush();
		s.clear();
		u = s.get( User.class, u.getId() );
		assertEquals( 1, u.getRoles().size() );
		s.getTransaction().rollback();
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
		s.close();
		
		s = openSession();
		s.getTransaction().begin();
		user = s.get( User.class, user.getId() );
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
		s.close();

		s = openSession();
		s.beginTransaction();
		Container result = s.get( Container.class, container1.id );
		assertNotNull( result );
		assertNotNull( result.items );
		assertEquals( 2, result.items.size() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		container1 = s.get( Container.class, container1.id );
		for ( Item item : container1.items ) {
			item.parent = null;
		}
		container1.items.clear();
		s.flush();
		s.createQuery( "delete Item" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
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
	
	@Entity( name = "Item" )
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
