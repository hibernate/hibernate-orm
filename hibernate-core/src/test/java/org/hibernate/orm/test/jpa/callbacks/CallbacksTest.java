/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.callbacks;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.hibernate.jpa.test.Cat;
import org.hibernate.jpa.test.Kitten;

import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
@DomainModel(annotatedClasses = {
		Cat.class,
		Kitten.class,
		Plant.class,
		Television.class,
		RemoteControl.class,
		Rythm.class
})
@SessionFactory
public class CallbacksTest {
	@Test
	public void testCallbackMethod(SessionFactoryScope scope) {
		int id = scope.fromTransaction(
				session -> {
					Cat c = new Cat();
					c.setName( "Kitty" );
					c.setDateOfBirth( new Date( 90, 11, 15 ) );
					session.persist( c );
					return c.getId();
				}
		);
		scope.inTransaction(
				session -> {
					Cat _c = session.find( Cat.class, id );
					assertFalse( _c.getAge() == 0 );
					_c.setName( "Tomcat" ); //update this entity
				}
		);
		scope.inTransaction(
				session -> {
					Cat _c = session.find( Cat.class, id );
					assertEquals( "Tomcat", _c.getName() );
				}
		);
	}

	@Test
	public void testEntityListener(SessionFactoryScope scope) {
		final class PV {
			PV(int version) {
				this.version = version;
			}
			private int version;
			private void set(int version) {
				this.version = version;
			}
			private int get() {
				return version;
			}
		}

		Cat c = new Cat();
		c.setName( "Kitty" );
		c.setLength( 12 );
		c.setDateOfBirth( new Date( 90, 11, 15 ) );
		PV previousVersion = new PV( c.getManualVersion() );

		scope.inTransaction(
				session -> session.persist( c )
		);
		scope.inTransaction(
				session -> {
					Cat _c = session.find( Cat.class, c.getId() );
					assertNotNull( _c.getLastUpdate() );
					assertTrue( previousVersion.get() < _c.getManualVersion() );
					assertEquals( 12, _c.getLength() );
					previousVersion.set( _c.getManualVersion() );
					_c.setName( "new name" );
				}
		);
		scope.inTransaction(
				session -> {
					Cat _c = session.find( Cat.class, c.getId() );
					assertTrue( previousVersion.get() < _c.getManualVersion() );
				}
		);
	}

	@Test
	public void testPostPersist(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Cat c = new Cat();
					c.setLength( 23 );
					c.setAge( 2 );
					c.setName( "Beetle" );
					c.setDateOfBirth( new Date() );
					session.persist( c );
				}
		);
		List ids = Cat.getIdList();
		Object id = Cat.getIdList().get( ids.size() - 1 );
		assertNotNull( id );
	}

	//Not a test since the spec did not make the proper change on listeners
	public void listenerAnnotation(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Translation tl = new Translation();
					session.getTransaction().begin();
					tl.setInto( "France" );
					session.persist( tl );
					tl = new Translation();
					tl.setInto( "Bimboland" );
					try {
						session.persist( tl );
						session.flush();
						fail( "Annotations annotated by a listener not used" );
					}
					catch (IllegalArgumentException e) {
						//success
					}
					finally {
						session.getTransaction().rollback();
						session.close();
					}
				}
		);
	}

	@Test
	public void testPrePersistOnCascade(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.getTransaction().begin();
					Television tv = new Television();
					RemoteControl rc = new RemoteControl();
					session.persist( tv );
					session.flush();
					tv.setControl( rc );
					tv.init();
					session.flush();
					assertNotNull( rc.getCreationDate() );
					session.getTransaction().rollback();
					session.close();
				}
		);
	}

	@Test
	public void testCallBackListenersHierarchy(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.getTransaction().begin();
					Television tv = new Television();
					session.persist( tv );
					tv.setName( "Myaio" );
					tv.init();
					session.flush();
					assertEquals( 1, tv.counter );
					session.getTransaction().rollback();
					session.close();
					assertEquals( 5, tv.communication );
					assertTrue( tv.isLast );
				}
		);
	}

	@Test
	public void testException(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.getTransaction().begin();
					Rythm r = new Rythm();
					try {
						session.persist( r );
						session.flush();
						fail( "should have raised an ArythmeticException:" );
					}
					catch (ArithmeticException e) {
						//success
					}
					catch (Exception e) {
						fail( "should have raised an ArythmeticException:" + e.getClass() );
					}

					session.getTransaction().rollback();
					session.close();
				}
		);
	}

	@Test
	public void testIdNullSetByPrePersist(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Plant plant = new Plant();
					plant.setName( "Origuna plantula gigantic" );
					session.getTransaction().begin();
					session.persist( plant );
					session.flush();
					session.getTransaction().rollback();
					session.close();
				}
		);
	}

	@Test
	@FailureExpected(reason = "collection change does not trigger an event", jiraKey = "EJB-288")
	public void testPostUpdateCollection(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					// create a cat
					Cat cat = new Cat();
					session.getTransaction().begin();
					cat.setLength( 23 );
					cat.setAge( 2 );
					cat.setName( "Beetle" );
					cat.setDateOfBirth( new Date() );
					session.persist( cat );
					session.getTransaction().commit();

					// assert it is persisted
					List ids = Cat.getIdList();
					Object id = Cat.getIdList().get( ids.size() - 1 );
					assertNotNull( id );

					// add a kitten to the cat - triggers PostCollectionRecreateEvent
					int postVersion = Cat.postVersion;
					session.getTransaction().begin();
					Kitten kitty = new Kitten();
					kitty.setName( "kitty" );
					List kittens = new ArrayList<Kitten>();
					kittens.add( kitty );
					cat.setKittens( kittens );
					session.getTransaction().commit();
					assertEquals( postVersion + 1, Cat.postVersion, "Post version should have been incremented." );

					// add another kitten - triggers PostCollectionUpdateEvent.
					postVersion = Cat.postVersion;
					session.getTransaction().begin();
					Kitten tom = new Kitten();
					tom.setName( "Tom" );
					cat.getKittens().add( tom );
					session.getTransaction().commit();
					assertEquals( postVersion + 1, Cat.postVersion, "Post version should have been incremented." );

					// delete a kitty - triggers PostCollectionUpdateEvent
					postVersion = Cat.postVersion;
					session.getTransaction().begin();
					cat.getKittens().remove( tom );
					session.getTransaction().commit();
					assertEquals( postVersion + 1, Cat.postVersion, "Post version should have been incremented." );

					// delete and recreate kittens - triggers PostCollectionRemoveEvent and PostCollectionRecreateEvent)
					postVersion = Cat.postVersion;
					session.getTransaction().begin();
					cat.setKittens( new ArrayList<Kitten>() );
					session.getTransaction().commit();
					assertEquals( postVersion + 2, Cat.postVersion, "Post version should have been incremented." );

					session.close();
				}
		);
	}

}
