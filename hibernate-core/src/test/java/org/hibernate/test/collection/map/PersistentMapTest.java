/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.collection.internal.PersistentMap;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Test various situations using a {@link PersistentMap}.
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class PersistentMapTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "collection/map/Mappings.hbm.xml" };
	}
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { User.class, UserData.class };
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void testWriteMethodDirtying() {
		Parent parent = new Parent( "p1" );
		Child child = new Child( "c1" );
		parent.getChildren().put( child.getName(), child );
		child.setParent( parent );
		Child otherChild = new Child( "c2" );

		Session session = openSession();
		session.beginTransaction();
		session.save( parent );
		session.flush();
		// at this point, the map on parent has now been replaced with a PersistentMap...
		PersistentMap children = ( PersistentMap ) parent.getChildren();

		Object old = children.put( child.getName(), child );
		assertTrue( old == child );
		assertFalse( children.isDirty() );

		old = children.remove( otherChild.getName() );
		assertNull( old );
		assertFalse( children.isDirty() );

		HashMap otherMap = new HashMap();
		otherMap.put( child.getName(), child );
		children.putAll( otherMap );
		assertFalse( children.isDirty() );

		otherMap = new HashMap();
		otherMap.put( otherChild.getName(), otherChild );
		children.putAll( otherMap );
		assertTrue( children.isDirty() );

		children.clearDirty();
		session.delete( child );
		children.clear();
		assertTrue( children.isDirty() );
		session.flush();

		children.clear();
		assertFalse( children.isDirty() );

		session.delete( parent );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPutAgainstUninitializedMap() {
		// prepare map owner...
		Session session = openSession();
		session.beginTransaction();
		Parent parent = new Parent( "p1" );
		session.save( parent );
		session.getTransaction().commit();
		session.close();

		// Now, reload the parent and test adding children
		session = openSession();
		session.beginTransaction();
		parent = ( Parent ) session.get( Parent.class, parent.getName() );
		parent.addChild( "c1" );
		parent.addChild( "c2" );
		session.getTransaction().commit();
		session.close();

		assertEquals( 2, parent.getChildren().size() );

		session = openSession();
		session.beginTransaction();
		session.delete( parent );
		session.getTransaction().commit();
		session.close();
	}

	@Test
    public void testRemoveAgainstUninitializedMap() {
        Parent parent = new Parent( "p1" );
        Child child = new Child( "c1" );
        parent.addChild( child );

        Session session = openSession();
        session.beginTransaction();
        session.save( parent );
        session.getTransaction().commit();
        session.close();

        // Now reload the parent and test removing the child
        session = openSession();
        session.beginTransaction();
        parent = ( Parent ) session.get( Parent.class, parent.getName() );
        Child child2 = ( Child ) parent.getChildren().remove( child.getName() );
		child2.setParent( null );
		assertNotNull( child2 );
		assertTrue( parent.getChildren().isEmpty() );
        session.getTransaction().commit();
        session.close();

		// Load the parent once again and make sure child is still gone
		//		then cleanup
        session = openSession();
        session.beginTransaction();
		parent = ( Parent ) session.get( Parent.class, parent.getName() );
		assertTrue( parent.getChildren().isEmpty() );
		session.delete( child2 );
		session.delete( parent );
        session.getTransaction().commit();
        session.close();
    }
	
	@Test
	@TestForIssue(jiraKey = "HHH-5732")
	public void testClearMap() {
		Session s = openSession();
		s.beginTransaction();
		
		User user = new User();
		UserData userData = new UserData();
		userData.user = user;
		user.userDatas.put( "foo", userData );
		s.persist( user );
		
		s.getTransaction().commit();
		s.clear();
		
		s.beginTransaction();
		
		user = (User) s.get( User.class, 1 );
	    user.userDatas.clear();
	    s.update( user );
		Query q = s.createQuery( "DELETE FROM " + UserData.class.getName() + " d WHERE d.user = :user" );
		q.setParameter( "user", user );
		q.executeUpdate();
		
		s.getTransaction().commit();
		s.clear();
		
		assertEquals( ( (User) s.get( User.class, user.id ) ).userDatas.size(), 0 );
		assertEquals( s.createQuery( "FROM " + UserData.class.getName() ).list().size(), 0 );
		
		s.close();
	}
	
	@Entity
	private static class User implements Serializable {
		@Id @GeneratedValue
		private Integer id;
		
		@OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.ALL)
		@MapKeyColumn(name = "name", nullable = true)
		private Map<String, UserData> userDatas = new HashMap<String, UserData>();
	}
	
	@Entity
	private static class UserData {
		@Id @GeneratedValue
		private Integer id;
		
		@ManyToOne
		@JoinColumn(name = "userId")
		private User user;
	}
}
