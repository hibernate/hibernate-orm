/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.transaction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@Jpa(
		annotatedClasses = {
				Book.class,
				CloseEntityManagerWithActiveTransactionTest.Container.class,
				CloseEntityManagerWithActiveTransactionTest.Box.class,
				CloseEntityManagerWithActiveTransactionTest.Muffin.class,
				CloseEntityManagerWithActiveTransactionTest.SmallBox.class
		},
		integrationSettings = {
				@Setting(name = AvailableSettings.CONNECTION_PROVIDER, value = "org.hibernate.testing.jta.JtaAwareConnectionProviderImpl"),
				@Setting(name = AvailableSettings.JPA_TRANSACTION_TYPE, value = "JTA"),
				@Setting(name = AvailableSettings.JPA_TRANSACTION_COMPLIANCE, value = "true")
		},
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.JTA_PLATFORM,
						provider = JtaPlatformSettingProvider.class
				)
		}
)
public class CloseEntityManagerWithActiveTransactionTest {

	@BeforeAll
	public void beforeAll(EntityManagerFactoryScope scope) throws Exception {
		// This makes sure that hbm2ddl runs before we start a transaction for a test
		// This is important for database that only support SNAPSHOT/SERIALIZABLE isolation,
		// because a test transaction still sees the state before the DDL executed
		scope.getEntityManagerFactory();
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) throws Exception {
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		try {
			transactionManager.begin();
			scope.inEntityManager(
					em -> {
						em.createQuery( "delete from Muffin" ).executeUpdate();
						em.createQuery( "delete from Box" ).executeUpdate();
						try {
							transactionManager.commit();
						}
						catch (Exception e) {
							throw new RuntimeException( e );
						}
					}
			);
		}
		catch (Exception e) {
			rollbackActiveTransaction( transactionManager );
			throw e;
		}
	}


	@Test
	@JiraKey(value = "HHH-10942")
	public void testPersistThenCloseWithAnActiveTransaction(EntityManagerFactoryScope scope) throws Exception {
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		try {
			transactionManager.begin();
			scope.inEntityManager(
					em -> {
						Box box = new Box();
						box.setColor( "red-and-white" );
						em.persist( box );
					}
			);
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}
		catch (Exception e) {
			rollbackActiveTransaction( transactionManager );
			throw e;
		}
		scope.inEntityManager(
				em -> {
					final List<Box> results = em.createQuery( "from Box", Box.class ).getResultList();
					assertThat( results.size(), is( 1 ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-11166")
	public void testMergeThenCloseWithAnActiveTransaction(EntityManagerFactoryScope scope) throws Exception {
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		try {
			transactionManager.begin();
			Box box = new Box();
			scope.inEntityManager(
					em -> {
						box.setColor( "red-and-white" );
						em.persist( box );
					}
			);

			transactionManager.commit();

			transactionManager.begin();
			scope.inEntityManager(
					em -> {
						Muffin muffin = new Muffin();
						muffin.setKind( "blueberry" );
						box.addMuffin( muffin );

						em.merge( box );
					}
			);
			transactionManager.commit();
		}
		catch (Exception e) {
			rollbackActiveTransaction( transactionManager );
			throw e;
		}

		scope.inEntityManager(
				em -> {
					final List<Box> boxes = em.createQuery( "from Box", Box.class ).getResultList();
					assertThat( boxes.size(), is( 1 ) );
					assertThat( boxes.get( 0 ).getMuffinSet().size(), is( 1 ) );
				}
		);
	}


	@Test
	@JiraKey(value = "HHH-11269")
	public void testMergeWithDeletionOrphanRemovalThenCloseWithAnActiveTransaction(EntityManagerFactoryScope scope)
			throws Exception {
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		try {
			transactionManager.begin();
			Muffin muffin = new Muffin();
			muffin.setKind( "blueberry" );
			SmallBox box = new SmallBox( muffin );
			box.setColor( "red-and-white" );
			scope.inEntityManager(
					em -> em.persist( box )
			);

			transactionManager.commit();

			transactionManager.begin();
			scope.inEntityManager(
					em -> {
						box.emptyBox();

						em.merge( box );
					}
			);
			transactionManager.commit();
		}
		catch (Exception e) {
			rollbackActiveTransaction( transactionManager );
			throw e;
		}

		scope.inEntityManager(
				em -> {
					final List<SmallBox> boxes = em.createQuery( "from SmallBox", SmallBox.class ).getResultList();
					assertThat( boxes.size(), is( 1 ) );
					assertTrue( boxes.get( 0 ).isEmpty() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-11166")
	public void testUpdateThenCloseWithAnActiveTransaction(EntityManagerFactoryScope scope) throws Exception {
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		try {
			transactionManager.begin();
			Box box = new Box();
			scope.inEntityManager(
					em -> {
						box.setColor( "red-and-white" );
						em.persist( box );
					}
			);

			transactionManager.commit();

			transactionManager.begin();
			scope.inEntityManager(
					em -> {
						Box result = em.find( Box.class, box.getId() );
						Muffin muffin = new Muffin();
						muffin.setKind( "blueberry" );
						result.addMuffin( muffin );
					}
			);
			transactionManager.commit();
		}
		catch (Exception e) {
			rollbackActiveTransaction( transactionManager );
			throw e;
		}

		scope.inEntityManager(
				em -> {
					final List<Box> boxes = em.createQuery( "from Box", Box.class ).getResultList();
					assertThat( boxes.size(), is( 1 ) );
					assertThat( boxes.get( 0 ).getMuffinSet().size(), is( 1 ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-11166")
	public void testRemoveThenCloseWithAnActiveTransaction(EntityManagerFactoryScope scope) throws Exception {
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		try {
			transactionManager.begin();
			Box box = new Box();
			scope.inEntityManager(
					entityManager -> {
						box.setColor( "red-and-white" );
						entityManager.persist( box );
						Muffin muffin = new Muffin();
						muffin.setKind( "blueberry" );
						box.addMuffin( muffin );
					}
			);

			transactionManager.commit();

			transactionManager.begin();
			scope.inEntityManager(
					entityManager -> {
						Box result = entityManager.find( Box.class, box.getId() );
						entityManager.remove( result );
					}
			);

			transactionManager.commit();
		}
		catch (Exception e) {
			rollbackActiveTransaction( transactionManager );
			throw e;
		}

		scope.inEntityManager(
				em -> {
					final List<Box> boxes = em.createQuery( "from Box", Box.class ).getResultList();
					assertThat( boxes.size(), is( 0 ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-11099")
	public void testCommitReleasesLogicalConnection(EntityManagerFactoryScope scope) throws Exception {
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		try {
			transactionManager.begin();
			final JdbcCoordinatorImpl jdbcCoordinator = scope.fromEntityManager(
					em -> {
						Box box = new Box();
						box.setColor( "red-and-white" );
						em.persist( box );
						final SessionImplementor session = em.unwrap( SessionImplementor.class );
						return (JdbcCoordinatorImpl) session.getJdbcCoordinator();
					}
			);

			transactionManager.commit();
			assertThat(
					"The logical connection is still open after commit",
					jdbcCoordinator.getLogicalConnection().isOpen(),
					is( false )
			);
		}
		catch (Exception e) {
			rollbackActiveTransaction( transactionManager );
			throw e;
		}
	}

	private void rollbackActiveTransaction(TransactionManager transactionManager) {
		try {
			switch ( transactionManager.getStatus() ) {
				case Status.STATUS_ACTIVE:
				case Status.STATUS_MARKED_ROLLBACK:
					transactionManager.rollback();
			}
		}
		catch (Exception exception) {
			//ignore exception
		}
	}

	@Entity(name = "Container")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class Container {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		private String color;

		public Long getId() {
			return id;
		}

		public String getColor() {
			return color;
		}

		public void setColor(String color) {
			this.color = color;
		}
	}

	@Entity(name = "Box")
	public static class Box extends Container {

		@OneToMany(mappedBy = "box", cascade = {
				CascadeType.MERGE,
				CascadeType.REMOVE,
				CascadeType.REFRESH,
				CascadeType.PERSIST
		}, fetch = FetchType.LAZY)
		private Set<Muffin> muffinSet;

		public Box() {
		}

		public void addMuffin(Muffin muffin) {
			muffin.setBox( this );
			if ( muffinSet == null ) {
				muffinSet = new HashSet<>();
			}
			muffinSet.add( muffin );
		}

		public Set<Muffin> getMuffinSet() {
			return muffinSet;
		}
	}

	@Entity(name = "SmallBox")
	public static class SmallBox extends Container {

		@OneToOne(cascade = {
				CascadeType.MERGE,
				CascadeType.REMOVE,
				CascadeType.REFRESH,
				CascadeType.PERSIST
		}, orphanRemoval = true)
		private Muffin muffin;

		public SmallBox() {
		}

		public SmallBox(Muffin muffin) {
			this.muffin = muffin;
		}

		public void emptyBox() {
			muffin = null;
		}

		public boolean isEmpty() {
			return muffin == null;
		}
	}

	@Entity(name = "Muffin")
	public static class Muffin {

		@Id
		@GeneratedValue
		private Long muffinId;

		@ManyToOne
		private Box box;

		private String kind;

		public Muffin() {
		}

		public Box getBox() {
			return box;
		}

		public void setBox(Box box) {
			this.box = box;
		}

		public void setKind(String kind) {
			this.kind = kind;
		}
	}
}
