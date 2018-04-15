/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.transaction;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
public class CloseEntityManagerWithActiveTransactionTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		TestingJtaBootstrap.prepare( options );
		options.put( AvailableSettings.TRANSACTION_TYPE, "JTA" );
		options.put( org.hibernate.cfg.AvailableSettings.JPA_TRANSACTION_COMPLIANCE, "true" );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Book.class, Container.class, Box.class, Muffin.class, SmallBox.class
		};
	}

	@After
	public void tearDown() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager em = getOrCreateEntityManager();
		try {
			em.createQuery( "delete from Muffin" ).executeUpdate();
			em.createQuery( "delete from Box" ).executeUpdate();
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}
		catch (Exception e) {
			final TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
			if ( transactionManager.getTransaction() != null && transactionManager.getTransaction()
					.getStatus() == Status.STATUS_ACTIVE ) {
				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
			}
			throw e;
		}
		finally {
			if ( em.isOpen() ) {
				em.close();
			}
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10942")
	public void testPersistThenCloseWithAnActiveTransaction() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager em = getOrCreateEntityManager();
		try {
			Box box = new Box();
			box.setColor( "red-and-white" );
			em.persist( box );
			em.close();
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}
		catch (Exception e) {
			final TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
			if ( transactionManager.getTransaction() != null && transactionManager.getTransaction()
					.getStatus() == Status.STATUS_ACTIVE ) {
				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
			}
			throw e;
		}
		finally {
			if ( em.isOpen() ) {
				em.close();
			}
		}
		em = getOrCreateEntityManager();
		try {
			final List results = em.createQuery( "from Box" ).getResultList();
			assertThat( results.size(), is( 1 ) );
		}
		finally {
			em.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11166")
	public void testMergeThenCloseWithAnActiveTransaction() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager em = getOrCreateEntityManager();
		try {
			Box box = new Box();
			box.setColor( "red-and-white" );
			em.persist( box );
			em.close();
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
			em = getOrCreateEntityManager();

			Muffin muffin = new Muffin();
			muffin.setKind( "blueberry" );
			box.addMuffin( muffin );

			em.merge( box );

			em.close();

			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}
		catch (Exception e) {
			final TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
			if ( transactionManager.getTransaction() != null && transactionManager.getTransaction()
					.getStatus() == Status.STATUS_ACTIVE ) {
				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
			}
			throw e;
		}
		finally {
			if ( em.isOpen() ) {
				em.close();
			}
		}
		em = getOrCreateEntityManager();
		try {
			final List<Box> boxes = em.createQuery( "from Box" ).getResultList();
			assertThat( boxes.size(), is( 1 ) );
			assertThat( boxes.get( 0 ).getMuffinSet().size(), is( 1 ) );
		}
		finally {
			em.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11269")
	public void testMergeWithDeletionOrphanRemovalThenCloseWithAnActiveTransaction() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager em = getOrCreateEntityManager();
		try {
			Muffin muffin = new Muffin();
			muffin.setKind( "blueberry" );
			SmallBox box = new SmallBox(muffin);
			box.setColor( "red-and-white" );
			em.persist( box );
			em.close();
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
			em = getOrCreateEntityManager();

			box.emptyBox();

			em.merge( box );

			em.close();

			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}
		catch (Exception e) {
			final TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
			if ( transactionManager.getTransaction() != null && transactionManager.getTransaction()
					.getStatus() == Status.STATUS_ACTIVE ) {
				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
			}
			throw e;
		}
		finally {
			if ( em.isOpen() ) {
				em.close();
			}
		}
		em = getOrCreateEntityManager();
		try {
			final List<SmallBox> boxes = em.createQuery( "from SmallBox" ).getResultList();
			assertThat( boxes.size(), is( 1 ) );
			assertTrue( boxes.get( 0 ).isEmpty());
		}
		finally {
			em.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11166")
	public void testUpdateThenCloseWithAnActiveTransaction() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager em = getOrCreateEntityManager();
		try {
			Box box = new Box();
			box.setColor( "red-and-white" );
			em.persist( box );
			em.close();
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
			em = getOrCreateEntityManager();
			box = em.find( Box.class, box.getId() );
			Muffin muffin = new Muffin();
			muffin.setKind( "blueberry" );
			box.addMuffin( muffin );

			em.close();
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}
		catch (Exception e) {
			final TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
			if ( transactionManager.getTransaction() != null && transactionManager.getTransaction()
					.getStatus() == Status.STATUS_ACTIVE ) {
				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
			}
			throw e;
		}
		finally {
			if ( em.isOpen() ) {
				em.close();
			}
		}
		em = getOrCreateEntityManager();
		try {
			final List<Box> boxes = em.createQuery( "from Box" ).getResultList();
			assertThat( boxes.size(), is( 1 ) );
			assertThat( boxes.get( 0 ).getMuffinSet().size(), is( 1 ) );
		}
		finally {
			em.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11166")
	public void testRemoveThenCloseWithAnActiveTransaction() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager em = getOrCreateEntityManager();
		try {
			Box box = new Box();
			box.setColor( "red-and-white" );
			em.persist( box );
			Muffin muffin = new Muffin();
			muffin.setKind( "blueberry" );
			box.addMuffin( muffin );
			em.close();
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();

			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
			em = getOrCreateEntityManager();
			box = em.find( Box.class, box.getId() );
			em.remove( box );

			em.close();
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
		}
		catch (Exception e) {
			final TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
			if ( transactionManager.getTransaction() != null && transactionManager.getTransaction()
					.getStatus() == Status.STATUS_ACTIVE ) {
				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
			}
			throw e;
		}
		finally {
			if ( em.isOpen() ) {
				em.close();
			}
		}
		em = getOrCreateEntityManager();
		try {
			final List<Box> boxes = em.createQuery( "from Box" ).getResultList();
			assertThat( boxes.size(), is( 0 ) );
		}
		finally {
			em.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11099")
	public void testCommitReleasesLogicalConnection() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager em = getOrCreateEntityManager();
		try {
			Box box = new Box();
			box.setColor( "red-and-white" );
			em.persist( box );
			final SessionImpl session = (SessionImpl) em.unwrap( Session.class );
			final JdbcCoordinatorImpl jdbcCoordinator = (JdbcCoordinatorImpl) session.getJdbcCoordinator();
			em.close();
			TestingJtaPlatformImpl.INSTANCE.getTransactionManager().commit();
			assertThat(
					"The logical connection is still open after commit",
					jdbcCoordinator.getLogicalConnection().isOpen(),
					is( false )
			);
		}
		catch (Exception e) {
			final TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
			if ( transactionManager.getTransaction() != null && transactionManager.getTransaction()
					.getStatus() == Status.STATUS_ACTIVE ) {
				TestingJtaPlatformImpl.INSTANCE.getTransactionManager().rollback();
			}
			throw e;
		}
		finally {
			if ( em.isOpen() ) {
				em.close();
			}
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

		@OneToOne(cascade = {CascadeType.MERGE,
				CascadeType.REMOVE,
				CascadeType.REFRESH,
				CascadeType.PERSIST
		}, orphanRemoval = true)
		private Muffin muffin;

		public SmallBox(){}

		public SmallBox(Muffin muffin) {
			this.muffin = muffin;
		}

		public void emptyBox(){
			muffin = null;
		}

		public boolean isEmpty(){
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


	@FunctionalInterface
	public interface Action{
		void execute();
	}
}
