/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.orm.test.jpa.Cat;
import org.hibernate.orm.test.jpa.Kitten;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
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

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCallbackMethod(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Cat c = new Cat();
					c.setName( "Kitty" );
					c.setDateOfBirth( new Date( 90, 11, 15 ) );
					entityManager.persist( c );
					entityManager.getTransaction().commit();

					entityManager.clear();

					entityManager.getTransaction().begin();
					c = entityManager.find( Cat.class, c.getId() );
					assertFalse( c.getAge() == 0 );
					c.setName( "Tomcat" ); //update this entity
					entityManager.getTransaction().commit();
					entityManager.clear();
					entityManager.getTransaction().begin();
					c = entityManager.find( Cat.class, c.getId() );
					assertEquals( "Tomcat", c.getName() );
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
		scope.inTransaction(
				entityManager -> {
					Cat c = new Cat();
					c.setName( "Kitty" );
					c.setLength( 12 );
					c.setDateOfBirth( new Date( 90, 11, 15 ) );
					PV previousVersion = new PV( c.getManualVersion() );
					entityManager.persist( c );
					entityManager.getTransaction().commit();

					entityManager.getTransaction().begin();
					c = entityManager.find( Cat.class, c.getId() );
					assertNotNull( c.getLastUpdate() );
					assertTrue( previousVersion.get() < c.getManualVersion() );
					assertEquals( 12, c.getLength() );
					previousVersion.set( c.getManualVersion() );
					c.setName( "new name" );
					entityManager.getTransaction().commit();
					entityManager.getTransaction().begin();
					c = entityManager.find( Cat.class, c.getId() );
					assertTrue( previousVersion.get() < c.getManualVersion() );
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
					}
					finally {
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
	}

	@Test
	@FailureExpected(reason = "collection change does not trigger an event", jiraKey = "EJB-288")
	public void testPostUpdateCollection(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						// create a cat
						Cat cat = new Cat();
						cat.setLength( 23 );
						cat.setAge( 2 );
						cat.setName( "Beetle" );
						cat.setDateOfBirth( new Date() );
						entityManager.getTransaction().begin();
						entityManager.persist( cat );
						entityManager.getTransaction().commit();
						// assert it is persisted
						List ids = Cat.getIdList();
						Object id = Cat.getIdList().get( ids.size() - 1 );
						assertNotNull( id );

						// add a kitten to the cat - triggers PostCollectionRecreateEvent
						int postVersion = Cat.postVersion;
						entityManager.getTransaction().begin();
						Kitten kitty = new Kitten();
						kitty.setName( "kitty" );
						List kittens = new ArrayList<Kitten>();
						kittens.add( kitty );
						cat.setKittens( kittens );
						entityManager.getTransaction().commit();
						assertEquals( postVersion + 1, Cat.postVersion, "Post version should have been incremented." );

						Kitten tom = new Kitten();
						tom.setName( "Tom" );

						// add another kitten - triggers PostCollectionUpdateEvent.
						postVersion = Cat.postVersion;
						entityManager.getTransaction().begin();
						cat.getKittens().add( tom );
						entityManager.getTransaction().commit();
						assertEquals( postVersion + 1, Cat.postVersion, "Post version should have been incremented." );

						// delete a kitty - triggers PostCollectionUpdateEvent
						postVersion = Cat.postVersion;
						entityManager.getTransaction().begin();
						cat.getKittens().remove( tom );
						entityManager.getTransaction().commit();
						assertEquals( postVersion + 1, Cat.postVersion, "Post version should have been incremented." );
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}
}
