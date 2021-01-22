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

import org.hibernate.jpa.test.Cat;
import org.hibernate.jpa.test.Kitten;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
@Jpa(annotatedClasses = {
		Cat.class,
		Kitten.class,
		Plant.class,
		Television.class,
		RemoteControl.class,
		Rythm.class
})
public class CallbacksTest {
	@Test
	public void testCallbackMethod(EntityManagerFactoryScope scope) {
		int id = scope.fromTransaction(
				entityManager -> {
					Cat c = new Cat();
					c.setName( "Kitty" );
					c.setDateOfBirth( new Date( 90, 11, 15 ) );
					entityManager.persist( c );
					return c.getId();
				}
		);
		scope.inTransaction(
				entityManager -> {
					Cat _c = entityManager.find( Cat.class, id );
					assertFalse( _c.getAge() == 0 );
					_c.setName( "Tomcat" ); //update this entity
				}
		);
		scope.inTransaction(
				entityManager -> {
					Cat _c = entityManager.find( Cat.class, id );
					assertEquals( "Tomcat", _c.getName() );
				}
		);

		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Cat" ).executeUpdate();
				}
		);
	}

	@Test
	public void testEntityListener(EntityManagerFactoryScope scope) {
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
				entityManager -> entityManager.persist( c )
		);
		scope.inTransaction(
				entityManager -> {
					Cat _c = entityManager.find( Cat.class, c.getId() );
					assertNotNull( _c.getLastUpdate() );
					assertTrue( previousVersion.get() < _c.getManualVersion() );
					assertEquals( 12, _c.getLength() );
					previousVersion.set( _c.getManualVersion() );
					_c.setName( "new name" );
				}
		);
		scope.inTransaction(
				entityManager -> {
					Cat _c = entityManager.find( Cat.class, c.getId() );
					assertTrue( previousVersion.get() < _c.getManualVersion() );
				}
		);

		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Cat" ).executeUpdate();
				}
		);
	}

	@Test
	public void testPostPersist(EntityManagerFactoryScope scope) {
		Cat c = new Cat();
		c.setLength( 23 );
		c.setAge( 2 );
		c.setName( "Beetle" );
		c.setDateOfBirth( new Date() );

		scope.inTransaction(
				entityManager -> {
					entityManager.persist( c );
				}
		);

		List ids = Cat.getIdList();
		Object id = Cat.getIdList().get( ids.size() - 1 );
		assertNotNull( id );

		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Cat" ).executeUpdate();
				}
		);
	}

	//Not a test since the spec did not make the proper change on listeners
	public void listenerAnnotation(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					Translation tl = new Translation();
					entityManager.getTransaction().begin();
					tl.setInto( "France" );
					entityManager.persist( tl );
					tl = new Translation();
					tl.setInto( "Bimboland" );
					try {
						entityManager.persist( tl );
						entityManager.flush();
						fail( "Annotations annotated by a listener not used" );
					}
					catch (IllegalArgumentException e) {
						//success
					}
					finally {
						entityManager.getTransaction().rollback();
						entityManager.close();
					}
				}
		);
	}

	@Test
	public void testPrePersistOnCascade(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
						Television tv = new Television();
						RemoteControl rc = new RemoteControl();
						entityManager.persist( tv );
						entityManager.flush();
						tv.setControl( rc );
						tv.init();
						entityManager.flush();
						assertNotNull( rc.getCreationDate() );
				}
		);

		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Television" ).executeUpdate();
				}
		);
	}

	@Test
	public void testCallBackListenersHierarchy(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Television tv = new Television();
					entityManager.persist( tv );
					tv.setName( "Myaio" );
					tv.init();
					entityManager.flush();
					assertEquals( 1, tv.counter );
					assertEquals( 5, tv.communication );
					assertTrue( tv.isLast );
				}
		);

		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Television" ).executeUpdate();
				}
		);
	}

	@Test
	public void testException(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					entityManager.getTransaction().begin();
					Rythm r = new Rythm();
					try {
						entityManager.persist( r );
						entityManager.flush();
						fail( "should have raised an ArythmeticException:" );
					}
					catch (ArithmeticException e) {
						//success
					}
					catch (Exception e) {
						fail( "should have raised an ArythmeticException:" + e.getClass() );
					} finally {
						entityManager.getTransaction().rollback();
						entityManager.close();
					}
				}
		);
	}

	@Test
	public void testIdNullSetByPrePersist(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Plant plant = new Plant();
					plant.setName( "Origuna plantula gigantic" );
					entityManager.persist( plant );
					entityManager.flush();
				}
		);

		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Plant" ).executeUpdate();
				}
		);
	}

	@Test
	@FailureExpected(reason = "collection change does not trigger an event", jiraKey = "EJB-288")
	public void testPostUpdateCollection(EntityManagerFactoryScope scope) {
		// create a cat
		Cat cat = new Cat();
		cat.setLength( 23 );
		cat.setAge( 2 );
		cat.setName( "Beetle" );
		cat.setDateOfBirth( new Date() );

		scope.inTransaction(
				entityManager -> {
					entityManager.persist( cat );
				}
		);

		// assert it is persisted
		List ids = Cat.getIdList();
		Object id = Cat.getIdList().get( ids.size() - 1 );
		assertNotNull( id );

		// add a kitten to the cat - triggers PostCollectionRecreateEvent
		scope.inTransaction(
				entityManager -> {
					int postVersion = Cat.postVersion;
					Kitten kitty = new Kitten();
					kitty.setName( "kitty" );
					List kittens = new ArrayList<Kitten>();
					kittens.add( kitty );
					cat.setKittens( kittens );
					assertEquals( postVersion + 1, Cat.postVersion, "Post version should have been incremented." );
				}
		);

		Kitten tom = new Kitten();
		tom.setName( "Tom" );
		scope.inTransaction(
				entityManager -> {
					// add another kitten - triggers PostCollectionUpdateEvent.
					int postVersion = Cat.postVersion;
					cat.getKittens().add( tom );
					assertEquals( postVersion + 1, Cat.postVersion, "Post version should have been incremented." );
				}
		);

		scope.inTransaction(
				entityManager -> {
					// delete a kitty - triggers PostCollectionUpdateEvent
					int postVersion = Cat.postVersion;
					cat.getKittens().remove( tom );
					assertEquals( postVersion + 1, Cat.postVersion, "Post version should have been incremented." );
				}
		);

		scope.inTransaction(
				entityManager -> {
					// delete and recreate kittens - triggers PostCollectionRemoveEvent and PostCollectionRecreateEvent)
					int postVersion = Cat.postVersion;
					cat.setKittens( new ArrayList<Kitten>() );
					assertEquals( postVersion + 2, Cat.postVersion, "Post version should have been incremented." );
				}
		);

		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Cat" ).executeUpdate();
					entityManager.createQuery( "delete from Kitten" ).executeUpdate();
				}
		);
	}
}
