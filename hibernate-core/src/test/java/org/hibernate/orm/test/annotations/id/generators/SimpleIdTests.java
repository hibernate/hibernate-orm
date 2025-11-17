/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.generators;

import java.util.UUID;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class SimpleIdTests {
	@Test
	@DomainModel(annotatedClasses = Entity1.class)
	@SessionFactory
	void testSimpleAutoFallback(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			session.persist( new Entity1( "1" ) );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = Entity2.class)
	@SessionFactory
	void testSimpleTableFallback(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			session.persist( new Entity2( "2" ) );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = Entity3.class)
	@SessionFactory
	void testSimpleNamedGenerator(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			session.persist( new Entity3( "3" ) );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = Entity4.class)
	@SessionFactory
	void testSimpleUuidFallback(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			session.persist( new Entity4( "4" ) );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = Entity5.class)
	@SessionFactory
	void testSimpleUuidAsStringFallback(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			session.persist( new Entity5( "5" ) );
		} );
	}

	@Entity(name="Entity1")
	@Table(name="Entity1")
	public static class Entity1 {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
		public Entity1() {
		}
		public Entity1(String name) {
			this.name = name;
		}
	}

	@Entity(name="Entity2")
	@Table(name="Entity2")
	public static class Entity2 {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE)
		private Integer id;
		private String name;
		public Entity2() {
		}
		public Entity2(String name) {
			this.name = name;
		}
	}

	@Entity(name="Entity3")
	@Table(name="Entity3")
	public static class Entity3 {
		@Id
		@GeneratedValue(generator = "increment")
		private Integer id;
		private String name;
		public Entity3() {
		}
		public Entity3(String name) {
			this.name = name;
		}
	}

	@Entity(name="Entity4")
	@Table(name="Entity4")
	public static class Entity4 {
		@Id
		@GeneratedValue
		private UUID id;
		private String name;
		public Entity4() {
		}
		public Entity4(String name) {
			this.name = name;
		}
	}

	@Entity(name="Entity5")
	@Table(name="Entity5")
	public static class Entity5 {
		@Id
		@GeneratedValue(strategy = GenerationType.UUID)
		private String id;
		private String name;
		public Entity5() {
		}
		public Entity5(String name) {
			this.name = name;
		}
	}
}
