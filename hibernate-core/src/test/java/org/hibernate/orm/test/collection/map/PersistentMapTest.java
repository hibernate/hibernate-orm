/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.map;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.collection.spi.PersistentMap;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
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
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/collection/map/Mappings.hbm.xml",
		annotatedClasses = {
				PersistentMapTest.User.class,
				PersistentMapTest.UserData.class,
				MultilingualString.class,
				MultilingualStringParent.class,
				PersistentMapTest.Address.class,
				PersistentMapTest.Detail.class
		}
)
@SessionFactory
public class PersistentMapTest {

	@Test
	@SuppressWarnings("unchecked")
	public void testWriteMethodDirtying(SessionFactoryScope scope) {
		Parent parent = new Parent( "p1" );
		Child child = new Child( "c1" );
		parent.getChildren().put( child.getName(), child );
		child.setParent( parent );
		Child otherChild = new Child( "c2" );

		scope.inTransaction(
				session -> {
					session.persist( parent );
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
					session.remove( child );
					children.clear();
					assertTrue( children.isDirty() );
					session.flush();

					children.clear();
					assertFalse( children.isDirty() );

					session.remove( parent );
				}
		);
	}

	@Test
	public void testPutAgainstUninitializedMap(SessionFactoryScope scope) {
		// prepare map owner...
		Parent p = new Parent( "p1" );
		scope.inTransaction(
				session -> {
					session.persist( p );
				}
		);

		// Now, reload the parent and test adding children
		Parent parent = scope.fromTransaction(
				session -> {
					Parent p1 = session.get( Parent.class, p.getName() );
					p1.addChild( "c1" );
					p1.addChild( "c2" );
					return p1;
				}
		);

		assertEquals( 2, parent.getChildren().size() );

		scope.inTransaction(
				session -> session.remove( parent )
		);
	}

	@Test
	public void testRemoveAgainstUninitializedMap(SessionFactoryScope scope) {
		Parent parent = new Parent( "p1" );
		Child child = new Child( "c1" );
		parent.addChild( child );

		scope.inTransaction(
				session -> session.persist( parent )
		);

		// Now reload the parent and test removing the child
		Child child2 = scope.fromTransaction(
				session -> {
					Parent p = session.get( Parent.class, parent.getName() );
					Child c2 = (Child) p.getChildren().remove( child.getName() );
					c2.setParent( null );
					assertNotNull( c2 );
					assertTrue( p.getChildren().isEmpty() );
					return c2;
				}
		);


		// Load the parent once again and make sure child is still gone
		//		then cleanup
		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, parent.getName() );
					assertTrue( p.getChildren().isEmpty() );
					session.remove( child2 );
					session.remove( p );
				}
		);
	}


	@Test
	public void testSelect(SessionFactoryScope scope) {
		User user = new User();
		scope.inTransaction(
				session -> {
					UserData userData = new UserData();
					userData.user = user;
					user.userDatas.put( "foo", userData );
					session.persist( user );
				}
		);

		scope.inTransaction(
				session -> {
					Query q = session.createQuery( "SELECT d.user FROM " + UserData.class.getName() + " d " );
					List<User> list = q.list();

					assertEquals( 1, list.size() );
					session.remove( list.get( 0 ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-5732")
	public void testClearMap(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.getTransaction().begin();
					try {
						User user = new User();
						UserData userData = new UserData();
						userData.user = user;
						user.userDatas.put( "foo", userData );
						session.persist( user );
						session.getTransaction().commit();
						session.clear();

						session.beginTransaction();


						user = session.get( User.class, user.id );
						user.userDatas.clear();
						session.merge( user );

						Query<UserData> q = session.createQuery( "DELETE FROM " + UserData.class.getName() + " d WHERE d.user = :user" );
						q.setParameter( "user", user );
						q.executeUpdate();

						session.getTransaction().commit();

						session.getTransaction().begin();
/*
select
		userdatas0_.userId as userid2_8_0_,
		userdatas0_.id as id1_8_0_,
		userdatas0_.name as name3_0_,
		userdatas0_.id as id1_8_1_,
		userdatas0_.userId as userid2_8_1_
	from
		UserData userdatas0_
	where
		userdatas0_.userId=1
 */
						assertEquals( 0, session.get( User.class, user.id ).userDatas.size() );
						assertEquals( 0, session.createQuery( "FROM " + UserData.class.getName() ).list().size() );
						session.createQuery( "delete " + User.class.getName() )
								.executeUpdate();
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-5393")
	public void testMapKeyColumnInEmbeddableElement(SessionFactoryScope scope) {
		MultilingualString m = new MultilingualString();
		scope.inTransaction(
				session -> {
					LocalizedString localizedString = new LocalizedString();
					localizedString.setLanguage( "English" );
					localizedString.setText( "name" );
					m.getMap().put( localizedString.getLanguage(), localizedString );
					localizedString = new LocalizedString();
					localizedString.setLanguage( "English Pig Latin" );
					localizedString.setText( "amenay" );
					m.getMap().put( localizedString.getLanguage(), localizedString );
					session.persist( m );
				}
		);

		scope.inTransaction(
				session -> {
					MultilingualString multilingualString = session.get( MultilingualString.class, m.getId() );
					assertEquals( 2, multilingualString.getMap().size() );
					LocalizedString localizedString = multilingualString.getMap().get( "English" );
					assertEquals( "English", localizedString.getLanguage() );
					assertEquals( "name", localizedString.getText() );
					localizedString = multilingualString.getMap().get( "English Pig Latin" );
					assertEquals( "English Pig Latin", localizedString.getLanguage() );
					assertEquals( "amenay", localizedString.getText() );
					session.remove( multilingualString );
				}
		);
	}

	@Test
	@JiraKey(value = "HQLPARSER-15")
	public void testJoinFetchElementCollectionWithParentSelect(SessionFactoryScope scope) {
		MultilingualStringParent parent = new MultilingualStringParent();
		scope.inTransaction(
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


		scope.inTransaction(
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
					session.remove( parent );
					session.remove( m );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-11038")
	public void testMapKeyColumnNonInsertableNonUpdatableBidirOneToMany(SessionFactoryScope scope) {
		User user = new User();
		scope.inTransaction(
				session -> {
					Address address = new Address();
					address.addressType = "email";
					address.addressText = "jane@doe.com";
					user.addresses.put( address.addressType, address );
					address.user = user;
					session.persist( user );
				}
		);

		scope.inTransaction(
				session -> {
					User u = session.get( User.class, user.id );
					u.addresses.clear();
				}
		);


		scope.inTransaction(
				session -> {
					User u = session.get( User.class, user.id );
					session.remove( u );
					session.createQuery( "delete from " + User.class.getName() ).executeUpdate();

				}
		);
	}

	@Test
	@JiraKey(value = "HHH-11038")
	public void testMapKeyColumnNonInsertableNonUpdatableUnidirOneToMany(SessionFactoryScope scope) {

		Integer userId = scope.fromTransaction(
				session -> {
					User user = new User();
					Detail detail = new Detail();
					detail.description = "desc";
					detail.detailType = "trivial";
					user.details.put( detail.detailType, detail );
					session.persist( user );
					return user.getId();
				}
		);

		scope.inTransaction(
				session -> {
					User user = session.get( User.class, userId);
					user.details.clear();
				}
		);

		scope.inTransaction(
				session -> {
					User user = session.get( User.class, userId );
					session.remove( user );
					session.createQuery( "delete from " + User.class.getName() ).executeUpdate();
				}
		);
	}

	@Entity(name = "MyUser")
	@Table(name = "MyUser")
	static class User implements Serializable {
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

		public Integer getId() {
			return id;
		}
	}

	@Entity(name = "UserData")
	@Table(name = "UserData")
	static class UserData {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		@JoinColumn(name = "userId")
		private User user;
	}

	@Entity(name = "Address")
	@Table(name = "Address")
	static class Address {
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

	@Entity(name = "Detail")
	@Table(name = "Detail")
	static class Detail {
		@Id
		@GeneratedValue
		private Integer id;

		@Column(nullable = false)
		private String detailType;

		private String description;
	}
}
