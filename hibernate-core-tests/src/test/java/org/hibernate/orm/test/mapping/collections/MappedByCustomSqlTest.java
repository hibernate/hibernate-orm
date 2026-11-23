package org.hibernate.orm.test.mapping.collections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// An unowned collection never writes its rows, and so may not have custom SQL
/// for writing them. The exception is an unowned one-to-many association with
/// an order column or map key column, which writes that column using an update.
@ServiceRegistry
@DomainModel(annotatedClasses = {
		MappedByCustomSqlTest.OrderedParent.class,
		MappedByCustomSqlTest.OrderedChild.class,
		MappedByCustomSqlTest.KeyedParent.class,
		MappedByCustomSqlTest.KeyedChild.class
})
@SessionFactory(useCollectingStatementInspector = true)
class MappedByCustomSqlTest {
	private static final String UPDATE_INDEX = "UPDATE OrderedChild SET idx = ? WHERE id = ?";
	private static final String UPDATE_KEY = "UPDATE KeyedChild SET code = ? WHERE id = ?";

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void manyToManyInsert(ServiceRegistryScope scope) {
		assertRejected( scope, "InsertGroup.parents", "@SQLInsert", InsertGroup.class );
	}

	@Test
	void manyToManyUpdate(ServiceRegistryScope scope) {
		assertRejected( scope, "UpdateGroup.parents", "@SQLUpdate", UpdateGroup.class );
	}

	@Test
	void manyToManyDelete(ServiceRegistryScope scope) {
		assertRejected( scope, "DeleteGroup.parents", "@SQLDelete", DeleteGroup.class );
	}

	@Test
	void manyToManyDeleteAll(ServiceRegistryScope scope) {
		assertRejected( scope, "DeleteAllGroup.parents", "@SQLDeleteAll", DeleteAllGroup.class );
	}

	@Test
	void oneToManyUpdateWithoutIndex(ServiceRegistryScope scope) {
		assertRejected( scope, "BagParent.children", "@SQLUpdate", BagParent.class, BagChild.class );
	}

	@Test
	void oneToManyUpdateWithMapKeyProperty(ServiceRegistryScope scope) {
		assertRejected( scope, "NamedParent.children", "@SQLUpdate", NamedParent.class, NamedChild.class );
	}

	@Test
	void oneToManyDeleteWithOrderColumn(ServiceRegistryScope scope) {
		assertRejected( scope, "ListParent.children", "@SQLDelete", ListParent.class, ListChild.class );
	}

	@Test
	void xmlManyToManyDeleteAll(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> new MetadataSources( scope.getRegistry() )
				.addResource( "org/hibernate/orm/test/mapping/collections/mapped-by-custom-sql.xml" )
				.buildMetadata() )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( "XmlGroup.parents' is 'mappedBy' another entity and may not specify '@SQLDeleteAll'" );
	}

	@Test
	void oneToManyUpdateWithOrderColumn(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var parent = new OrderedParent();
			parent.id = 1L;
			session.persist( parent );
			for ( long id = 1; id <= 2; id++ ) {
				final var child = new OrderedChild();
				child.id = id;
				child.parent = parent;
				parent.children.add( child );
				session.persist( child );
			}
		} );
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final var children = session.find( OrderedParent.class, 1L ).children;
			children.add( 0, children.remove( 1 ) );
		} );
		assertThat( inspector.getSqlQueries() ).contains( UPDATE_INDEX );
		scope.inTransaction( session ->
				assertThat( session.find( OrderedParent.class, 1L ).children )
						.extracting( child -> child.id )
						.containsExactly( 2L, 1L ) );
	}

	@Test
	void oneToManyUpdateWithMapKeyColumn(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final var parent = new KeyedParent();
			parent.id = 1L;
			session.persist( parent );
			final var child = new KeyedChild();
			child.id = 1L;
			child.parent = parent;
			parent.children.put( "first", child );
			session.persist( child );
		} );
		assertThat( inspector.getSqlQueries() ).contains( UPDATE_KEY );
		scope.inTransaction( session ->
				assertThat( session.find( KeyedParent.class, 1L ).children ).containsOnlyKeys( "first" ) );
	}

	private static void assertRejected(
			ServiceRegistryScope scope, String role, String annotation, Class<?>... entityClasses) {
		assertThatThrownBy( () -> new MetadataSources( scope.getRegistry() )
				.addAnnotatedClasses( entityClasses )
				.buildMetadata() )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( role + "' is 'mappedBy' another entity and may not specify '" + annotation + "'" );
	}

	@Entity(name = "InsertGroup")
	static class InsertGroup {
		@Id
		long id;
		@ManyToMany
		Set<InsertGroup> children;
		@ManyToMany(mappedBy = "children")
		@SQLInsert(sql = "insert into InsertGroup_InsertGroup (parents_id, children_id) values (?, ?)")
		Set<InsertGroup> parents;
	}

	@Entity(name = "UpdateGroup")
	static class UpdateGroup {
		@Id
		long id;
		@ManyToMany
		Set<UpdateGroup> children;
		@ManyToMany(mappedBy = "children")
		@SQLUpdate(sql = "update UpdateGroup_UpdateGroup set children_id = ? where parents_id = ? and children_id = ?")
		Set<UpdateGroup> parents;
	}

	@Entity(name = "DeleteGroup")
	static class DeleteGroup {
		@Id
		long id;
		@ManyToMany
		Set<DeleteGroup> children;
		@ManyToMany(mappedBy = "children")
		@SQLDelete(sql = "delete from DeleteGroup_DeleteGroup where parents_id = ? and children_id = ?")
		Set<DeleteGroup> parents;
	}

	@Entity(name = "DeleteAllGroup")
	static class DeleteAllGroup {
		@Id
		long id;
		@ManyToMany
		Set<DeleteAllGroup> children;
		@ManyToMany(mappedBy = "children")
		@SQLDeleteAll(sql = "delete from DeleteAllGroup_DeleteAllGroup where children_id = ?")
		Set<DeleteAllGroup> parents;
	}

	static class XmlGroup {
		long id;
		Set<XmlGroup> children;
		Set<XmlGroup> parents;
	}

	@Entity(name = "BagParent")
	static class BagParent {
		@Id
		long id;
		@OneToMany(mappedBy = "parent")
		@SQLUpdate(sql = "update BagChild set parent_id = ? where id = ?")
		List<BagChild> children;
	}

	@Entity(name = "BagChild")
	static class BagChild {
		@Id
		long id;
		@ManyToOne
		BagParent parent;
	}

	@Entity(name = "NamedParent")
	static class NamedParent {
		@Id
		long id;
		@OneToMany(mappedBy = "parent")
		@MapKey(name = "name")
		@SQLUpdate(sql = "update NamedChild set name = ? where id = ?")
		Map<String, NamedChild> children;
	}

	@Entity(name = "NamedChild")
	static class NamedChild {
		@Id
		long id;
		String name;
		@ManyToOne
		NamedParent parent;
	}

	@Entity(name = "ListParent")
	static class ListParent {
		@Id
		long id;
		@OneToMany(mappedBy = "parent")
		@OrderColumn(name = "idx")
		@SQLDelete(sql = "update ListChild set parent_id = null, idx = null where parent_id = ? and id = ?")
		List<ListChild> children;
	}

	@Entity(name = "ListChild")
	static class ListChild {
		@Id
		long id;
		@ManyToOne
		ListParent parent;
	}

	@Entity(name = "OrderedParent")
	static class OrderedParent {
		@Id
		long id;
		@OneToMany(mappedBy = "parent")
		@OrderColumn(name = "idx")
		@SQLUpdate(sql = UPDATE_INDEX)
		List<OrderedChild> children = new ArrayList<>();
	}

	@Entity(name = "OrderedChild")
	static class OrderedChild {
		@Id
		long id;
		@ManyToOne
		OrderedParent parent;
	}

	@Entity(name = "KeyedParent")
	static class KeyedParent {
		@Id
		long id;
		@OneToMany(mappedBy = "parent")
		@MapKeyColumn(name = "code")
		@SQLUpdate(sql = UPDATE_KEY)
		Map<String, KeyedChild> children = new HashMap<>();
	}

	@Entity(name = "KeyedChild")
	static class KeyedChild {
		@Id
		long id;
		@ManyToOne
		KeyedParent parent;
	}
}
