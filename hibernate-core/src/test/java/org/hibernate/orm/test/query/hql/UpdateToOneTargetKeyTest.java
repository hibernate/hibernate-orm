package org.hibernate.orm.test.query.hql;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.hibernate.Session;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SoftDelete;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A bulk update which assigns the key of the target of a to-one writes the
 * foreign key, even when the target entity is joined for a read of the
 * association, and never the primary key of the owner.
 */
@DomainModel(annotatedClasses = {
		UpdateToOneTargetKeyTest.Base.class,
		UpdateToOneTargetKeyTest.Parent.class,
		UpdateToOneTargetKeyTest.Child.class,
		UpdateToOneTargetKeyTest.SoftParent.class,
		UpdateToOneTargetKeyTest.SoftChild.class,
		UpdateToOneTargetKeyTest.Target.class,
		UpdateToOneTargetKeyTest.Holder.class,
		UpdateToOneTargetKeyTest.Ref.class,
		UpdateToOneTargetKeyTest.KeyTarget.class,
		UpdateToOneTargetKeyTest.KeyHolder.class,
		UpdateToOneTargetKeyTest.Placed.class
})
@SessionFactory
@JiraKey("HHH-13988")
class UpdateToOneTargetKeyTest {

	@Entity(name = "Base")
	@Table(name = "bases")
	static class Base {
		@Id
		Long objectId;
		@ManyToOne
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(name = "fk")
		Base childA;
	}

	@Entity(name = "Parent")
	@Table(name = "parents")
	@SQLRestriction("deleted = false")
	static class Parent {
		@Id
		Long id;
		boolean deleted;
	}

	@Entity(name = "Child")
	@Table(name = "children")
	static class Child {
		@Id
		Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_id")
		Parent parent;
	}

	@Entity(name = "SoftParent")
	@Table(name = "soft_parents")
	@SoftDelete
	static class SoftParent {
		@Id
		Long id;
	}

	@Entity(name = "SoftChild")
	@Table(name = "soft_children")
	static class SoftChild {
		@Id
		Long id;
		@ManyToOne
		@JoinColumn(name = "parent_id")
		SoftParent parent;
	}

	@Embeddable
	static class TargetId implements Serializable {
		Integer a;
		Integer b;

		TargetId() {
		}

		TargetId(Integer a, Integer b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public boolean equals(Object object) {
			return object instanceof TargetId that && Objects.equals( a, that.a ) && Objects.equals( b, that.b );
		}

		@Override
		public int hashCode() {
			return Objects.hash( a, b );
		}
	}

	@Entity(name = "Target")
	@Table(name = "targets")
	@SQLRestriction("deleted = false")
	static class Target {
		@EmbeddedId
		TargetId id;
		boolean deleted;
	}

	/// Has columns with the same names as the columns of the key of its target.
	@Entity(name = "Holder")
	@Table(name = "holders")
	static class Holder {
		@Id
		Long id;
		Integer a;
		Integer b;
		@ManyToOne
		@JoinColumns({
				@JoinColumn(name = "target_a", referencedColumnName = "a"),
				@JoinColumn(name = "target_b", referencedColumnName = "b")
		})
		Target target;
	}

	@Entity(name = "Ref")
	@Table(name = "refs")
	static class Ref {
		@Id
		Long id;
	}

	@Embeddable
	static class KeyTargetId implements Serializable {
		Long num;
		@ManyToOne
		@JoinColumn(name = "key_ref")
		Ref reference;

		@Override
		public boolean equals(Object object) {
			return object instanceof KeyTargetId that && Objects.equals( num, that.num )
				&& Objects.equals( reference.id, that.reference.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( num, reference.id );
		}
	}

	/// Has an association in its embedded identifier.
	@Entity(name = "KeyTarget")
	@Table(name = "key_targets")
	@SQLRestriction("deleted = false")
	static class KeyTarget {
		@EmbeddedId
		KeyTargetId id;
		boolean deleted;
	}

	/// Has a column with the same name as the column of the association in the key of
	/// its target.
	@Entity(name = "KeyHolder")
	@Table(name = "key_holders")
	static class KeyHolder {
		@Id
		Long id;
		@Column(name = "key_ref")
		Long keyRef;
		@ManyToOne
		@JoinColumns({
				@JoinColumn(name = "target_num", referencedColumnName = "num"),
				@JoinColumn(name = "target_ref", referencedColumnName = "key_ref")
		})
		KeyTarget target;
	}

	@Embeddable
	static class Location {
		@ManyToOne
		@JoinColumn(name = "parent_id")
		Parent parent;
	}

	/// Has an association in an embeddable.
	@Entity(name = "Placed")
	@Table(name = "placed")
	static class Placed {
		@Id
		Long id;
		@Embedded
		Location location;
	}

	@AfterEach
	void dropData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void notFound(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var five = base( session, 5L, null );
			final var six = base( session, 6L, null );
			base( session, 10L, six );
			assertThat( five ).isNotNull();
		} );
		final int count = scope.fromTransaction( session -> session.createMutationQuery(
						"update Base b set b.childA.objectId = :newId where b.childA.objectId = :oldId" )
				.setParameter( "newId", 5L )
				.setParameter( "oldId", 6L )
				.executeUpdate() );
		assertThat( count ).isEqualTo( 1 );
		assertThat( rows( scope, "select objectId, fk from bases order by objectId" ) )
				.containsExactly( "[5, null]", "[6, null]", "[10, 5]" );
	}

	@Test
	void restriction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var visible = parent( session, 1L, false );
			parent( session, 2L, false );
			final var hidden = parent( session, 3L, true );
			child( session, 1L, visible );
			child( session, 2L, hidden );
		} );
		final int count = scope.fromTransaction( session -> session.createMutationQuery(
						"update Child c set c.parent.id = 2 where c.id in (1, 2)" )
				.executeUpdate() );
		assertThat( count ).isEqualTo( 2 );
		assertThat( rows( scope, "select id, parent_id from children order by id" ) )
				.containsExactly( "[1, 2]", "[2, 2]" );
	}

	@Test
	void softDelete(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var visible = softParent( session, 1L );
			final var deleted = softParent( session, 2L );
			softChild( session, 1L, deleted );
			assertThat( visible ).isNotNull();
		} );
		scope.inTransaction( session -> session.createMutationQuery( "delete from SoftParent where id = 2" )
				.executeUpdate() );
		final int count = scope.fromTransaction( session -> session.createMutationQuery(
						"update SoftChild c set c.parent.id = 1 where c.id = 1" )
				.executeUpdate() );
		assertThat( count ).isEqualTo( 1 );
		assertThat( rows( scope, "select id, parent_id from soft_children" ) )
				.containsExactly( "[1, 1]" );
	}

	@Test
	void embeddedId(SessionFactoryScope scope) {
		createHolders( scope );
		final int count = scope.fromTransaction( session -> session.createMutationQuery(
						"update Holder h set h.target.id = :id where h.id in (1, 2)" )
				.setParameter( "id", new TargetId( 2, 2 ) )
				.executeUpdate() );
		assertThat( count ).isEqualTo( 2 );
		assertThat( rows( scope, "select id, a, b, target_a, target_b from holders order by id" ) )
				.containsExactly( "[1, 10, 20, 2, 2]", "[2, 10, 20, 2, 2]" );
	}

	@Test
	void embeddedIdComponent(SessionFactoryScope scope) {
		createHolders( scope );
		final int count = scope.fromTransaction( session -> session.createMutationQuery(
						"update Holder h set h.target.id.a = 2 where h.id in (1, 2)" )
				.executeUpdate() );
		assertThat( count ).isEqualTo( 2 );
		assertThat( rows( scope, "select id, a, b, target_a, target_b from holders order by id" ) )
				.containsExactly( "[1, 10, 20, 2, 1]", "[2, 10, 20, 2, 3]" );
	}

	@Test
	void associationInEmbeddedId(SessionFactoryScope scope) {
		createKeyHolders( scope );
		final int count = scope.fromTransaction( session -> session.createMutationQuery(
						"update KeyHolder h set h.target.id.reference = :ref where h.id in (1, 2)" )
				.setParameter( "ref", session.getReference( Ref.class, 2L ) )
				.executeUpdate() );
		assertThat( count ).isEqualTo( 2 );
		assertThat( rows( scope, "select id, key_ref, target_num, target_ref from key_holders order by id" ) )
				.containsExactly( "[1, 99, 7, 2]", "[2, 99, 8, 2]" );
	}

	@Test
	void keyOfAssociationInEmbeddedId(SessionFactoryScope scope) {
		createKeyHolders( scope );
		final int count = scope.fromTransaction( session -> session.createMutationQuery(
						"update KeyHolder h set h.target.id.reference.id = 2 where h.id in (1, 2)" )
				.executeUpdate() );
		assertThat( count ).isEqualTo( 2 );
		assertThat( rows( scope, "select id, key_ref, target_num, target_ref from key_holders order by id" ) )
				.containsExactly( "[1, 99, 7, 2]", "[2, 99, 8, 2]" );
	}

	@Test
	void associationInEmbeddable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var visible = parent( session, 1L, false );
			parent( session, 2L, false );
			final var hidden = parent( session, 3L, true );
			placed( session, 1L, visible );
			placed( session, 2L, hidden );
		} );
		final int count = scope.fromTransaction( session -> session.createMutationQuery(
						"update Placed p set p.location.parent.id = 2 where p.id in (1, 2)" )
				.executeUpdate() );
		assertThat( count ).isEqualTo( 2 );
		assertThat( rows( scope, "select id, parent_id from placed order by id" ) )
				.containsExactly( "[1, 2]", "[2, 2]" );
	}

	private static void createKeyHolders(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var one = ref( session, 1L );
			final var two = ref( session, 2L );
			final var visible = keyTarget( session, 7L, one, false );
			final var hidden = keyTarget( session, 8L, one, true );
			// the targets of the assignments
			keyTarget( session, 7L, two, false );
			keyTarget( session, 8L, two, false );
			keyHolder( session, 1L, visible );
			keyHolder( session, 2L, hidden );
		} );
	}

	private static Ref ref(Session session, Long id) {
		final var ref = new Ref();
		ref.id = id;
		session.persist( ref );
		return ref;
	}

	private static KeyTarget keyTarget(Session session, Long num, Ref reference, boolean deleted) {
		final var target = new KeyTarget();
		target.id = new KeyTargetId();
		target.id.num = num;
		target.id.reference = reference;
		target.deleted = deleted;
		session.persist( target );
		return target;
	}

	private static void keyHolder(Session session, Long id, KeyTarget target) {
		final var holder = new KeyHolder();
		holder.id = id;
		holder.keyRef = 99L;
		holder.target = target;
		session.persist( holder );
	}

	private static void placed(Session session, Long id, Parent parent) {
		final var placed = new Placed();
		placed.id = id;
		placed.location = new Location();
		placed.location.parent = parent;
		session.persist( placed );
	}

	private static void createHolders(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var visible = target( session, 1, 1, false );
			final var hidden = target( session, 3, 3, true );
			// the targets of the assignments
			target( session, 2, 1, false );
			target( session, 2, 2, false );
			target( session, 2, 3, false );
			holder( session, 1L, visible );
			holder( session, 2L, hidden );
		} );
	}

	private static Target target(Session session, int a, int b, boolean deleted) {
		final var target = new Target();
		target.id = new TargetId( a, b );
		target.deleted = deleted;
		session.persist( target );
		return target;
	}

	private static void holder(Session session, Long id, Target target) {
		final var holder = new Holder();
		holder.id = id;
		holder.a = 10;
		holder.b = 20;
		holder.target = target;
		session.persist( holder );
	}

	private static List<String> rows(SessionFactoryScope scope, String sql) {
		return scope.fromTransaction( session -> session.createNativeQuery( sql, Object[].class ).getResultList()
				.stream().map( Arrays::toString ).toList() );
	}

	private static Base base(Session session, Long id, Base childA) {
		final var base = new Base();
		base.objectId = id;
		base.childA = childA;
		session.persist( base );
		return base;
	}

	private static Parent parent(Session session, Long id, boolean deleted) {
		final var parent = new Parent();
		parent.id = id;
		parent.deleted = deleted;
		session.persist( parent );
		return parent;
	}

	private static void child(Session session, Long id, Parent parent) {
		final var child = new Child();
		child.id = id;
		child.parent = parent;
		session.persist( child );
	}

	private static SoftParent softParent(Session session, Long id) {
		final var parent = new SoftParent();
		parent.id = id;
		session.persist( parent );
		return parent;
	}

	private static void softChild(Session session, Long id, SoftParent parent) {
		final var child = new SoftChild();
		child.id = id;
		child.parent = parent;
		session.persist( child );
	}
}
