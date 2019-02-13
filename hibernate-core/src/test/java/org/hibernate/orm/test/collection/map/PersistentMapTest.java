/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.map;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.collection.internal.PersistentMap;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test various situations using a {@link PersistentMap}.
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 * @author Gail Badner
 */
public class PersistentMapTest extends SessionFactoryBasedFunctionalTest {
	@Override
	public String[] getHmbMappingFiles() {
		return new String[] { "collection/map/Mappings.hbm.xml" };
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				User.class,
				UserData.class,
				MultilingualString.class,
				MultilingualStringParent.class,
				Address.class,
				Detail.class
		};
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void testWriteMethodDirtying() {
		Parent parent = new Parent( "p1" );
		Child child = new Child( "c1" );
		parent.getChildren().put( child.getName(), child );
		child.setParent( parent );
		Child otherChild = new Child( "c2" );

		inTransaction(
				session -> {
					session.save( parent );
					session.flush();
					// at this point, the map on parent has now been replaced with a PersistentMap...
					PersistentMap children = (PersistentMap) parent.getChildren();

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
				}
		);
	}

	@Test
	public void testPutAgainstUninitializedMap() {
		// prepare map owner...
		Parent parent = new Parent( "p1" );
		inTransaction(
				session -> {
					session.save( parent );
				}
		);

		// Now, reload the parent and test adding children
		Parent savedParent = inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parent.getName() );
					p.addChild( "c1" );
					p.addChild( "c2" );
					return p;
				}
		);

		assertEquals( 2, savedParent.getChildren().size() );

		inTransaction(
				session -> {
					session.delete( parent );
				}
		);
	}

	@Test
	public void testRemoveAgainstUninitializedMap() {
		final Parent parent = new Parent( "p1" );
		Child child = new Child( "c1" );
		parent.addChild( child );

		inTransaction(
				session -> {
					session.save( parent );
				}
		);

		// Now reload the parent and test removing the child
		Child removedChild = inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parent.getName() );
					Child child2 = (Child) p.getChildren().remove( child.getName() );
					child2.setParent( null );
					assertNotNull( child2 );
					assertTrue( p.getChildren().isEmpty() );
					return child2;
				}
		);

		// Load the parent once again and make sure child is still gone
		//		then cleanup
		inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parent.getName() );
					assertTrue( p.getChildren().isEmpty() );
					session.delete( removedChild );
					session.delete( p );

				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-5732")
	public void testClearMap() {
		inSession(
				session -> {
					try {
						session.beginTransaction();
						User user = new User();
						UserData userData = new UserData();
						userData.user = user;
						user.userDatas.put( "foo", userData );
						session.persist( user );

						session.getTransaction().commit();
						session.clear();

						session.beginTransaction();

						user = session.get( User.class, 1 );
						user.userDatas.clear();
						session.update( user );
						Query q = session.createQuery( "DELETE FROM " + UserData.class.getName() + " d WHERE d.user = :user" );
						q.setParameter( "user", user );
						q.executeUpdate();

						session.getTransaction().commit();

						session.getTransaction().begin();

						assertEquals( session.get( User.class, user.id ).userDatas.size(), 0 );
						assertEquals( session.createQuery( "FROM " + UserData.class.getName() ).list().size(), 0 );
						session.createQuery( "delete " + User.class.getName() ).executeUpdate();

						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

	}

	@Test
	@TestForIssue(jiraKey = "HHH-5393")
	public void testMapKeyColumnInEmbeddableElement() {
		MultilingualString multilingualString = new MultilingualString();
		inTransaction(
				session -> {
					LocalizedString localizedString = new LocalizedString();
					localizedString.setLanguage( "English" );
					localizedString.setText( "name" );
					multilingualString.getMap().put( localizedString.getLanguage(), localizedString );
					localizedString = new LocalizedString();
					localizedString.setLanguage( "English Pig Latin" );
					localizedString.setText( "amenay" );
					multilingualString.getMap().put( localizedString.getLanguage(), localizedString );
					session.persist( multilingualString );
				}
		);

		inTransaction(
				session -> {
					MultilingualString m = session.get( MultilingualString.class, multilingualString.getId() );
					assertEquals( 2, m.getMap().size() );
					LocalizedString localizedString = m.getMap().get( "English" );
					assertEquals( "English", localizedString.getLanguage() );
					assertEquals( "name", localizedString.getText() );
					localizedString = m.getMap().get( "English Pig Latin" );
					assertEquals( "English Pig Latin", localizedString.getLanguage() );
					assertEquals( "amenay", localizedString.getText() );
					session.delete( m );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HQLPARSER-15")
	public void testJoinFetchElementCollectionWithParentSelect() {
		MultilingualStringParent parent = new MultilingualStringParent();

		inTransaction(
				session -> {
					MultilingualString m = new MultilingualString();
					LocalizedString localizedString = new LocalizedString();
					localizedString.setLanguage( "English" );
					localizedString.setText( "name" );
					m.getMap().put( localizedString.getLanguage(), localizedString );
					localizedString = new LocalizedString();
					localizedString.setLanguage( "English Pig Latin" );
					localizedString.setText( "amenay" );
					m.getMap().put( localizedString.getLanguage(), localizedString );

					parent.setString( m );

					session.persist( m );
					session.persist( parent );
				}
		);

		inTransaction(
				session -> {
					MultilingualString m = session.createQuery(
							"SELECT s FROM MultilingualStringParent parent " +
									"JOIN parent.string s " +
									"JOIN FETCH s.map", MultilingualString.class )
							.getSingleResult();
					assertEquals( 2, m.getMap().size() );
					LocalizedString localizedString = m.getMap().get( "English" );
					assertEquals( "English", localizedString.getLanguage() );
					assertEquals( "name", localizedString.getText() );
					localizedString = m.getMap().get( "English Pig Latin" );
					assertEquals( "English Pig Latin", localizedString.getLanguage() );
					assertEquals( "amenay", localizedString.getText() );
					session.delete( parent );
					session.delete( m );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11038")
	public void testMapKeyColumnNonInsertableNonUpdatableBidirOneToMany() {
		final User user = new User();
		inTransaction(
				session -> {
					Address address = new Address();
					address.addressType = "email";
					address.addressText = "jane@doe.com";
					user.addresses.put( address.addressType, address );
					address.user = user;
					session.persist( user );
				}
		);

		inTransaction(
				session -> {
					User u = session.get( User.class, user.id );
					u.addresses.clear();

				}
		);

		inTransaction(
				session -> {
					User u = session.get( User.class, user.id );
					session.delete( u );
					session.createQuery( "delete from " + User.class.getName() ).executeUpdate();
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11038")
	public void testMapKeyColumnNonInsertableNonUpdatableUnidirOneToMany() {

		final User user = new User();
		inTransaction(
				session -> {
					Detail detail = new Detail();
					detail.description = "desc";
					detail.detailType = "trivial";
					user.details.put( detail.detailType, detail );
					session.persist( user );
				}
		);

		inTransaction(
				session -> {
					User u = session.get( User.class, user.id );
					u.details.clear();
				}
		);

		inTransaction(
				session -> {
					User u = session.get( User.class, user.id );
					session.delete( u );
					session.createQuery( "delete from " + User.class.getName() ).executeUpdate();
				}
		);
	}

	@Entity
	@Table(name = "MyUser")
	private static class User implements Serializable {
		@Id
		@GeneratedValue
		private Integer id;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.ALL)
		@MapKeyColumn(name = "name", nullable = true)
		private Map<String, UserData> userDatas = new HashMap<>();

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.ALL)
		@MapKeyColumn(name = "addressType", insertable = false, updatable = false)
		private Map<String, Address> addresses = new HashMap<>();

		@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		@MapKeyColumn(name = "detailType", insertable = false, updatable = false)
		@JoinColumn
		private Map<String, Detail> details = new HashMap<>();
	}

	@Entity
	@Table(name = "UserData")
	private static class UserData {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		@JoinColumn(name = "userId")
		private User user;
	}

	@Entity
	@Table(name = "Address")
	private static class Address {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		@JoinColumn(name = "userId")
		private User user;

		@Column(nullable = false)
		private String addressType;

		@Column(nullable = false)
		private String addressText;
	}

	@Entity
	@Table(name = "Detail")
	private static class Detail {
		@Id
		@GeneratedValue
		private Integer id;

		@Column(nullable = false)
		private String detailType;

		private String description;
	}
}
