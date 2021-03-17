/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking.DirtyCheckEnhancementContext;
import org.hibernate.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking.NoDirtyCheckEnhancementContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Andrea Boriero
 */
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ DirtyCheckEnhancementContext.class, NoDirtyCheckEnhancementContext.class })
public class SharingReferenceTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyStatisticsSupport( true );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( Ceo.class );
		sources.addAnnotatedClass( Manager.class );
		sources.addAnnotatedClass( Supervisor.class );
		sources.addAnnotatedClass( Worker.class );
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					Ceo ceo = new Ceo();
					ceo.setName( "Jill" );
					session.persist( ceo );

					Manager m1 = new Manager();
					m1.setName( "Jane" );
					m1.setCeo( ceo );
					session.persist( m1 );

					Manager m2 = new Manager();
					m2.setName( "Jannet" );
					m2.setCeo( ceo );
					session.persist( m2 );

					Supervisor s1 = new Supervisor();
					s1.setName( "Bob" );
					s1.getManagers().add( m1 );
					s1.getManagers().add( m2 ); //comment out this line and the test will pass
					s1.setCeo( ceo );
					session.persist( s1 );

					Worker worker = new Worker();
					worker.setName( "James" );
					worker.setSupervisor( s1 );
					session.persist( worker );
				}
		);
	}

	@Test
	public void testFind() {
		inTransaction(
				session -> {
					Ceo ceo = session.find( Ceo.class, 1L );
					assertEquals( "Jill", ceo.getName() );

					Worker worker = session.find( Worker.class, 1L );
					assertEquals( worker.getName(), "James" );

					Supervisor supervisor = worker.getSupervisor();
					Manager manager = supervisor.getManagers().get( 0 );
					assertSame( ceo, manager.getCeo() );
					assertSame( ceo, supervisor.getCeo() );
				}
		);

	}

	@Entity(name = "Ceo")
	public static class Ceo {

		private long id;
		private String name;

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ceqSeq")
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Manager")
	public static class Manager {

		private long id;
		private String name;
		private Ceo ceo;

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "managerSeq")
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		public Ceo getCeo() {
			return ceo;
		}

		public void setCeo(Ceo ceo) {
			this.ceo = ceo;
		}
	}

	@Entity(name = "Supervisor")
	public static class Supervisor {

		private long id;
		private String name;
		private List<Manager> managers = new ArrayList<>();
		private Ceo ceo;

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "supervisorSeq")
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		public List<Manager> getManagers() {
			return managers;
		}

		public void setManagers(List<Manager> managers) {
			this.managers = managers;
		}

		@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		public Ceo getCeo() {
			return ceo;
		}

		public void setCeo(Ceo ceo) {
			this.ceo = ceo;
		}
	}

	@Entity(name = "Worker")
	public static class Worker {

		private long id;
		private String name;
		private Supervisor supervisor;

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "workerSeq")
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		public Supervisor getSupervisor() {
			return supervisor;
		}

		public void setSupervisor(Supervisor supervisor) {
			this.supervisor = supervisor;
		}
	}
}
