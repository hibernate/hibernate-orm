/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking.DirtyCheckEnhancementContext;
import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking.NoDirtyCheckEnhancementContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				SharingReferenceTest.Ceo.class,
				SharingReferenceTest.Manager.class,
				SharingReferenceTest.Supervisor.class,
				SharingReferenceTest.Worker.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "false"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false"),
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "false"),
				@Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true"),
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ DirtyCheckEnhancementContext.class, NoDirtyCheckEnhancementContext.class })
public class SharingReferenceTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
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
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction(
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
