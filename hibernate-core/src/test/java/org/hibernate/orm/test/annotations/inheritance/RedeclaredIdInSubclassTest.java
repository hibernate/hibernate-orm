package org.hibernate.orm.test.annotations.inheritance;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The identifier of an entity hierarchy is declared exactly once, by the
 * root entity or by one of its mapped superclasses. An {@code @Id} or
 * {@code @EmbeddedId} declared further down the hierarchy is rejected.
 */
@JiraKey("HHH-21110")
@ServiceRegistry
public class RedeclaredIdInSubclassTest {

	@Test
	public void singleTable(ServiceRegistryScope scope) {
		assertRejected( scope, SubItem.class, SubItem.class, "id", Item.class );
	}

	@Test
	public void joined(ServiceRegistryScope scope) {
		assertRejected( scope, JoinedSubItem.class, JoinedSubItem.class, "id", JoinedItem.class );
	}

	@Test
	public void tablePerClass(ServiceRegistryScope scope) {
		assertRejected( scope, UnionSubItem.class, UnionSubItem.class, "id", UnionItem.class );
	}

	@Test
	public void differentName(ServiceRegistryScope scope) {
		assertRejected( scope, OtherIdSubItem.class, OtherIdSubItem.class, "otherId", Item.class );
	}

	@Test
	public void embeddedId(ServiceRegistryScope scope) {
		assertRejected( scope, EmbeddedIdSubItem.class, EmbeddedIdSubItem.class, "pk", Item.class );
	}

	@Test
	public void overriddenGetter(ServiceRegistryScope scope) {
		assertRejected( scope, PropSubItem.class, PropSubItem.class, "id", PropItem.class );
	}

	@Test
	public void explicitPropertyAccess(ServiceRegistryScope scope) {
		assertRejected( scope, AccessSubItem.class, AccessSubItem.class, "id", Item.class );
	}

	@Test
	public void intermediateMappedSuperclass(ServiceRegistryScope scope) {
		assertRejected( scope, MiddleSubItem.class, Middle.class, "id", Item.class, Middle.class );
	}

	private static void assertRejected(
			ServiceRegistryScope scope,
			Class<?> subclass,
			Class<?> declaringClass,
			String attribute,
			Class<?>... superclasses) {
		final var sources = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClasses( superclasses )
				.addAnnotatedClass( subclass );
		final var exception = assertThrows( AnnotationException.class, sources::buildMetadata );
		final String message = exception.getMessage();
		assertTrue( message.contains( "Entity '" + subclass.getName() + "'" ), message );
		assertTrue( message.contains( "'" + attribute + "' of '" + declaringClass.getName() + "'" ), message );
	}

	@Entity(name = "Item")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class Item {
		@Id
		private Long id;
	}

	@Entity(name = "SubItem")
	public static class SubItem extends Item {
		@Id
		private Long id;
	}

	@Entity(name = "OtherIdSubItem")
	public static class OtherIdSubItem extends Item {
		@Id
		private Long otherId;
	}

	@Embeddable
	public record Pk(long a, long b) {
	}

	@Entity(name = "EmbeddedIdSubItem")
	public static class EmbeddedIdSubItem extends Item {
		@EmbeddedId
		private Pk pk;
	}

	@Entity(name = "AccessSubItem")
	@Access(AccessType.PROPERTY)
	public static class AccessSubItem extends Item {
		private Long id;

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@MappedSuperclass
	public static class Middle extends Item {
		@Id
		private Long id;
	}

	@Entity(name = "MiddleSubItem")
	public static class MiddleSubItem extends Middle {
		String name;
	}

	@Entity(name = "JoinedItem")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class JoinedItem {
		@Id
		private Long id;
	}

	@Entity(name = "JoinedSubItem")
	public static class JoinedSubItem extends JoinedItem {
		@Id
		private Long id;
	}

	@Entity(name = "UnionItem")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class UnionItem {
		@Id
		private Long id;
	}

	@Entity(name = "UnionSubItem")
	public static class UnionSubItem extends UnionItem {
		@Id
		private Long id;
	}

	@Entity(name = "PropItem")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class PropItem {
		private Long id;

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "PropSubItem")
	public static class PropSubItem extends PropItem {
		@Id
		@Override
		public Long getId() {
			return super.getId();
		}
	}
}
