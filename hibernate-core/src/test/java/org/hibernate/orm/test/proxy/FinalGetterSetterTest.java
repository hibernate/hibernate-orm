/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.proxy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gail Badner
 */
@JiraKey(value = "HHH-13891")
@DomainModel(
		annotatedClasses = {
				FinalGetterSetterTest.EntityWithFinalClass.class,
				FinalGetterSetterTest.EntityWithFinalIdGetter.class,
				FinalGetterSetterTest.EntityWithFinalIdSetter.class,
				FinalGetterSetterTest.EntityWithFinalVersionGetter.class,
				FinalGetterSetterTest.EntityWithFinalVersionSetter.class,
				FinalGetterSetterTest.EntityWithFinalPropertyGetter.class,
				FinalGetterSetterTest.EntityWithFinalPropertySetter.class
		}
)
@SessionFactory
public class FinalGetterSetterTest {

	@Test
	public void testEntityWithFinalClass(SessionFactoryScope scope) {
		scope.inTransaction( session ->
									 assertNull( session.get( EntityWithFinalClass.class, 999 ) )
		);

		try {
			scope.inTransaction( session ->
										 session.load( EntityWithFinalClass.class, 999 )
			);
			fail( "Should have thrown ObjectNotFoundException" );
		}
		catch (ObjectNotFoundException expected) {
		}

		scope.inTransaction( session -> {
			final EntityWithFinalClass entity = new EntityWithFinalClass();
			entity.id = 1;
			entity.name = "An Entity";
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final EntityWithFinalClass entity = session.load( EntityWithFinalClass.class, 1 );
			assertNotNull( entity );
			assertTrue( Hibernate.isInitialized( entity ) );
		} );
	}

	@Test
	public void testEntityWithFinalIdGetter(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						assertNull( session.get( EntityWithFinalIdGetter.class, 999 ) )
		);

		try {
			scope.inTransaction(
					session ->
							session.load( EntityWithFinalIdGetter.class, 999 )
			);
			fail( "Should have thrown ObjectNotFoundException" );
		}
		catch (ObjectNotFoundException expected) {
		}

		scope.inTransaction(
				session -> {
					final EntityWithFinalIdGetter entity = new EntityWithFinalIdGetter();
					entity.id = 1;
					entity.name = "An Entity";
					session.persist( entity );
				} );

		scope.inTransaction(
				session -> {
					final EntityWithFinalIdGetter entity = session.load( EntityWithFinalIdGetter.class, 1 );
					assertNotNull( entity );
					assertTrue( Hibernate.isInitialized( entity ) );
				} );
	}

	@Test
	public void testEntityWithFinalIdSetter(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						assertNull( session.get( EntityWithFinalIdSetter.class, 999 ) )
		);

		try {
			scope.inTransaction(
					session ->
							session.load( EntityWithFinalIdSetter.class, 999 )
			);
			fail( "Should have thrown ObjectNotFoundException" );
		}
		catch (ObjectNotFoundException expected) {
		}

		scope.inTransaction(
				session -> {
					final EntityWithFinalIdSetter entity = new EntityWithFinalIdSetter();
					entity.id = 1;
					entity.name = "An Entity";
					session.persist( entity );
				} );

		scope.inTransaction(
				session -> {
					final EntityWithFinalIdSetter entity = session.load( EntityWithFinalIdSetter.class, 1 );
					assertNotNull( entity );
					assertTrue( Hibernate.isInitialized( entity ) );
				} );
	}

	@Test
	public void testEntityWithFinalVersionGetter(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						assertNull( session.get( EntityWithFinalVersionGetter.class, 999 ) )
		);

		try {
			scope.inTransaction(
					session ->
							session.load( EntityWithFinalVersionGetter.class, 999 )
			);
			fail( "Should have thrown ObjectNotFoundException" );
		}
		catch (ObjectNotFoundException expected) {
		}

		scope.inTransaction(
				session -> {
					final EntityWithFinalVersionGetter entity = new EntityWithFinalVersionGetter();
					entity.id = 1;
					entity.name = "An Entity";
					session.persist( entity );
				} );

		scope.inTransaction(
				session -> {
					final EntityWithFinalVersionGetter entity = session.load( EntityWithFinalVersionGetter.class, 1 );
					assertNotNull( entity );
					assertTrue( Hibernate.isInitialized( entity ) );
				} );
	}

	@Test
	public void testEntityWithFinalVersionSetter(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						assertNull( session.get( EntityWithFinalVersionSetter.class, 999 ) )
		);

		try {
			scope.inTransaction(
					session ->
							session.load( EntityWithFinalVersionSetter.class, 999 )
			);
			fail( "Should have thrown ObjectNotFoundException" );
		}
		catch (ObjectNotFoundException expected) {
		}

		scope.inTransaction(
				session -> {
					final EntityWithFinalVersionSetter entity = new EntityWithFinalVersionSetter();
					entity.id = 1;
					entity.name = "An Entity";
					session.persist( entity );
				} );

		scope.inTransaction(
				session -> {
					final EntityWithFinalVersionSetter entity = session.load( EntityWithFinalVersionSetter.class, 1 );
					assertNotNull( entity );
					assertTrue( Hibernate.isInitialized( entity ) );
				} );
	}

	@Test
	public void testEntityWithFinalPropertyGetter(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						assertNull( session.get( EntityWithFinalPropertyGetter.class, 999 ) )
		);

		try {
			scope.inTransaction(
					session ->
							session.load( EntityWithFinalPropertyGetter.class, 999 )
			);
			fail( "Should have thrown ObjectNotFoundException" );
		}
		catch (ObjectNotFoundException expected) {
		}

		scope.inTransaction(
				session -> {
					final EntityWithFinalPropertyGetter entity = new EntityWithFinalPropertyGetter();
					entity.id = 1;
					entity.name = "An Entity";
					session.persist( entity );
				} );

		scope.inTransaction(
				session -> {
					final EntityWithFinalPropertyGetter entity = session.load( EntityWithFinalPropertyGetter.class, 1 );
					assertNotNull( entity );
					assertTrue( Hibernate.isInitialized( entity ) );
				} );
	}

	@Test
	public void testEntityWithFinalPropertySetter(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						assertNull( session.get( EntityWithFinalPropertySetter.class, 999 ) )
		);

		try {
			scope.inTransaction(
					session ->
							session.load( EntityWithFinalPropertySetter.class, 999 )
			);
			fail( "Should have thrown ObjectNotFoundException" );
		}
		catch (ObjectNotFoundException expected) {
		}

		scope.inTransaction(
				session -> {
					final EntityWithFinalPropertySetter entity = new EntityWithFinalPropertySetter();
					entity.id = 1;
					entity.name = "An Entity";
					session.persist( entity );
				} );

		scope.inTransaction(
				session -> {
					final EntityWithFinalPropertySetter entity = session.load( EntityWithFinalPropertySetter.class, 1 );
					assertNotNull( entity );
					assertTrue( Hibernate.isInitialized( entity ) );
				} );
	}

	@Entity(name = "EntityWithFinalClass")
	public static final class EntityWithFinalClass {

		@Id
		private int id;

		@Version
		@Column(name = "ver")
		private int version;

		private String name;

		public final int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "EntityWithFinalIdGetter")
	public static class EntityWithFinalIdGetter {

		@Id
		private int id;

		@Version
		@Column(name = "ver")
		private int version;

		private String name;

		public final int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "EntityWithFinalIdSetter")
	public static class EntityWithFinalIdSetter {
		@Id
		private int id;

		@Version
		@Column(name = "ver")
		private int version;

		private String name;

		public int getId() {
			return id;
		}

		public final void setId(int id) {
			this.id = id;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "EntityWithFinalVersionGetter")
	public static class EntityWithFinalVersionGetter {
		@Id
		private int id;

		@Version
		@Column(name = "ver")
		private int version;

		private String name;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public final int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "EntityWithFinalVersionSetter")
	public static class EntityWithFinalVersionSetter {
		@Id
		private int id;

		@Version
		@Column(name = "ver")
		private int version;

		private String name;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getVersion() {
			return version;
		}

		public final void setVersion(int version) {
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "EntityWithFinalPropertyGetter")
	public static class EntityWithFinalPropertyGetter {
		@Id
		private int id;

		@Version
		@Column(name = "ver")
		private int version;

		private String name;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}

		public final String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "EntityWithFinalPropertySetter")
	public static class EntityWithFinalPropertySetter {
		@Id
		private int id;

		@Version
		@Column(name = "ver")
		private int version;

		private String name;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public final void setName(String name) {
			this.name = name;
		}
	}
}
