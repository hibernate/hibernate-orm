/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.engine.spi.Managed;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.util.ReflectionUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@DomainModel(annotatedClasses = {
		FinalFieldTest.EntityWithFinalField.class,
		FinalFieldTest.EntityWithFinalId.class,
		FinalFieldTest.EntityWithFinalIdClass.class,
		FinalFieldTest.EntityWithFinalIdClassRecord.class,
		FinalFieldTest.EntityWithFinalEmbeddedId.class,
		FinalFieldTest.ParentWithFinalOneToMany.class,
		FinalFieldTest.ChildOfParent.class,
		FinalFieldTest.AuthorWithFinalManyToMany.class,
		FinalFieldTest.BookWithFinalManyToMany.class,
		FinalFieldTest.EntityWithFinalElementCollection.class
})
@SessionFactory(useCollectingStatementInspector = true)
@BytecodeEnhanced(runNotEnhancedAsWell = true)
public class FinalFieldTest {

	@BeforeAll
	static void assumeEnhancedOrNoIllegalFinalFieldMutation() {
		boolean enhanced = Managed.class.isAssignableFrom( EntityWithFinalField.class );

		assumeFalse( ReflectionUtil.isFinalFieldMutationDenied() && !enhanced,
				"Skipping non-enhanced variant when JVM denies final field mutation" );
	}

	@Test
	public void finalFieldNotUpdatable(SessionFactoryScope scope) {
		var statementInspector = scope.getCollectingStatementInspector();

		var persistedEntity = new EntityWithFinalField( "foo", "foo".toCharArray() );
		persistedEntity.setName( "Some name" );
		scope.inTransaction( s -> {
			s.persist( persistedEntity );
		} );

		statementInspector.clear();

		scope.inTransaction( s -> {
			var entity = s.find( EntityWithFinalField.class, persistedEntity.getId() );
			entity.setName( "Updated name" );
		} );

		assertEquals( 2, statementInspector.getSqlQueries().size() );
		String select = statementInspector.getSqlQueries().get( 0 );
		String update = statementInspector.getSqlQueries().get( 1 );
		assertTrue( select.startsWith( "select" ) );
		assertTrue( update.startsWith( "update" ) );
		assertFalse( update.contains( "immutable" ) );
		assertTrue( update.contains( "mutable" ) );
	}

	@Test
	public void finalFieldNotDirtyChecked(SessionFactoryScope scope) {
		var statementInspector = scope.getCollectingStatementInspector();

		var persistedEntity = new EntityWithFinalField( "foo", "foo".toCharArray() );
		persistedEntity.setName( "Some name" );
		scope.inTransaction( s -> {
			s.persist( persistedEntity );
		} );

		statementInspector.clear();

		scope.inTransaction( s -> {
			var entity = s.find( EntityWithFinalField.class, persistedEntity.getId() );
			// Modify the final field via reflection
			try {
				var field = EntityWithFinalField.class.getDeclaredField( "immutable" );
				field.setAccessible( true );
				field.set( entity, "bar" );
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		} );

		assertEquals( 1, statementInspector.getSqlQueries().size() );
		assertTrue( statementInspector.getSqlQueries().get( 0 ).startsWith( "select" ) );

		statementInspector.clear();

		scope.inTransaction( s -> {
			var entity = s.find( EntityWithFinalField.class, persistedEntity.getId() );
			entity.mutable[0] = 'b';
			entity.mutable[1] = 'a';
			entity.mutable[2] = 'r';
		} );

		assertEquals( 2, statementInspector.getSqlQueries().size() );
		assertTrue( statementInspector.getSqlQueries().get( 0 ).startsWith( "select" ) );
		assertTrue( statementInspector.getSqlQueries().get( 1 ).startsWith( "update" ) );
	}

	@Entity(name = "EntityWithFinalField")
	public static class EntityWithFinalField {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private final String immutable;

		private final char[] mutable;

		protected EntityWithFinalField() {
			this.immutable = null;
			this.mutable = null;
		}

		public EntityWithFinalField(String immutable, char[] mutable) {
			this.immutable = immutable;
			this.mutable = mutable;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getImmutable() {
			return immutable;
		}
	}

	@Test
	public void testFinalIdField(SessionFactoryScope scope) {
		Long entityId = scope.fromTransaction( s -> {
			EntityWithFinalId entity = new EntityWithFinalId( 1L, "test" );
			s.persist( entity );
			return entity.getId();
		} );

		scope.inTransaction( s -> {
			EntityWithFinalId loaded = s.find( EntityWithFinalId.class, entityId );
			assertNotNull( loaded );
			assertEquals( entityId, loaded.getId() );
			assertEquals( "test", loaded.getName() );
		} );
	}

	@Test
	public void testFinalIdClassFields(SessionFactoryScope scope) {
		assumeFalse( Managed.class.isAssignableFrom( EntityWithFinalIdClass.class ),
				"https://hibernate.atlassian.net/browse/HHH-20542" );

		scope.inTransaction( s -> {
			EntityWithFinalIdClass entity = new EntityWithFinalIdClass( 1L, 2L, "test" );
			s.persist( entity );
		} );

		scope.inTransaction( s -> {
			EntityWithFinalIdClass loaded = s.find( EntityWithFinalIdClass.class, new CompositeId( 1L, 2L ) );
			assertNotNull( loaded );
			assertEquals( 1L, loaded.getId1() );
			assertEquals( 2L, loaded.getId2() );
			assertEquals( "test", loaded.getName() );
		} );
	}

	@Test
	public void testFinalIdClassFieldsAsRecord(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			EntityWithFinalIdClassRecord entity = new EntityWithFinalIdClassRecord( 1L, 2L, "test" );
			s.persist( entity );
		} );

		scope.inTransaction( s -> {
			EntityWithFinalIdClassRecord loaded = s.find( EntityWithFinalIdClassRecord.class, new CompositeIdRecord( 1L, 2L ) );
			assertNotNull( loaded );
			assertEquals( 1L, loaded.getId1() );
			assertEquals( 2L, loaded.getId2() );
			assertEquals( "test", loaded.getName() );
		} );
	}

	@Test
	public void testFinalEmbeddedIdField(SessionFactoryScope scope) {
		EmbeddedCompositeId id = new EmbeddedCompositeId( 1L, 2L );

		scope.inTransaction( s -> {
			EntityWithFinalEmbeddedId entity = new EntityWithFinalEmbeddedId( id, "test" );
			s.persist( entity );
		} );

		scope.inTransaction( s -> {
			EntityWithFinalEmbeddedId loaded = s.find( EntityWithFinalEmbeddedId.class, id );
			assertNotNull( loaded );
			assertEquals( id, loaded.getId() );
			assertEquals( "test", loaded.getName() );
		} );
	}

	@Test
	public void testFinalOneToManyField(SessionFactoryScope scope) {
		Long parentId = scope.fromTransaction( s -> {
			ParentWithFinalOneToMany parent = new ParentWithFinalOneToMany( "parent" );
			ChildOfParent child1 = new ChildOfParent( "child1", parent );
			ChildOfParent child2 = new ChildOfParent( "child2", parent );
			parent.getChildren().add( child1 );
			parent.getChildren().add( child2 );
			s.persist( parent );
			return parent.getId();
		} );

		scope.inTransaction( s -> {
			ParentWithFinalOneToMany loaded = s.find( ParentWithFinalOneToMany.class, parentId );
			assertNotNull( loaded );
			assertEquals( "parent", loaded.getName() );
			assertEquals( 2, loaded.getChildren().size() );
		} );
	}

	@Test
	public void testFinalManyToManyField(SessionFactoryScope scope) {
		Long authorId = scope.fromTransaction( s -> {
			AuthorWithFinalManyToMany author = new AuthorWithFinalManyToMany( "author" );
			BookWithFinalManyToMany book1 = new BookWithFinalManyToMany( "book1" );
			BookWithFinalManyToMany book2 = new BookWithFinalManyToMany( "book2" );
			author.getBooks().add( book1 );
			author.getBooks().add( book2 );
			book1.getAuthors().add( author );
			book2.getAuthors().add( author );
			s.persist( author );
			return author.getId();
		} );

		scope.inTransaction( s -> {
			AuthorWithFinalManyToMany loaded = s.find( AuthorWithFinalManyToMany.class, authorId );
			assertNotNull( loaded );
			assertEquals( "author", loaded.getName() );
			assertEquals( 2, loaded.getBooks().size() );
		} );
	}

	@Test
	public void testFinalElementCollectionField(SessionFactoryScope scope) {
		Long entityId = scope.fromTransaction( s -> {
			EntityWithFinalElementCollection entity = new EntityWithFinalElementCollection( "entity" );
			entity.getTags().add( "tag1" );
			entity.getTags().add( "tag2" );
			entity.getTags().add( "tag3" );
			s.persist( entity );
			return entity.getId();
		} );

		scope.inTransaction( s -> {
			EntityWithFinalElementCollection loaded = s.find( EntityWithFinalElementCollection.class, entityId );
			assertNotNull( loaded );
			assertEquals( "entity", loaded.getName() );
			assertEquals( 3, loaded.getTags().size() );
			assertTrue( loaded.getTags().contains( "tag1" ) );
			assertTrue( loaded.getTags().contains( "tag2" ) );
			assertTrue( loaded.getTags().contains( "tag3" ) );
		} );
	}

	@Entity(name = "EntityWithFinalId")
	public static class EntityWithFinalId {

		@Id
		private final Long id;

		private String name;

		protected EntityWithFinalId() {
			this.id = null;
		}

		public EntityWithFinalId(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "EntityWithFinalIdClass")
	@IdClass(FinalFieldTest.CompositeId.class)
	public static class EntityWithFinalIdClass {

		@Id
		private final Long id1;

		@Id
		private final Long id2;

		private String name;

		protected EntityWithFinalIdClass() {
			this.id1 = null;
			this.id2 = null;
		}

		public EntityWithFinalIdClass(Long id1, Long id2, String name) {
			this.id1 = id1;
			this.id2 = id2;
			this.name = name;
		}

		public Long getId1() {
			return id1;
		}

		public Long getId2() {
			return id2;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class CompositeId implements Serializable {
		private final Long id1;
		private final Long id2;

		protected CompositeId() {
			this.id1 = null;
			this.id2 = null;
		}

		public CompositeId(Long id1, Long id2) {
			this.id1 = id1;
			this.id2 = id2;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			CompositeId that = (CompositeId) o;
			return Objects.equals( id1, that.id1 ) && Objects.equals( id2, that.id2 );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id1, id2 );
		}
	}

	@Entity(name = "EntityWithFinalIdClassRecord")
	@IdClass(FinalFieldTest.CompositeIdRecord.class)
	public static class EntityWithFinalIdClassRecord {

		@Id
		private final Long id1;

		@Id
		private final Long id2;

		private String name;

		protected EntityWithFinalIdClassRecord() {
			this.id1 = null;
			this.id2 = null;
		}

		public EntityWithFinalIdClassRecord(Long id1, Long id2, String name) {
			this.id1 = id1;
			this.id2 = id2;
			this.name = name;
		}

		public Long getId1() {
			return id1;
		}

		public Long getId2() {
			return id2;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public record CompositeIdRecord(Long id1, Long id2) {}

	@Entity(name = "EntityWithFinalEmbeddedId")
	public static class EntityWithFinalEmbeddedId {

		@EmbeddedId
		private final EmbeddedCompositeId id;

		private String name;

		protected EntityWithFinalEmbeddedId() {
			this.id = null;
		}

		public EntityWithFinalEmbeddedId(EmbeddedCompositeId id, String name) {
			this.id = id;
			this.name = name;
		}

		public EmbeddedCompositeId getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class EmbeddedCompositeId implements Serializable {
		private final Long part1;
		private final Long part2;

		protected EmbeddedCompositeId() {
			this.part1 = null;
			this.part2 = null;
		}

		public EmbeddedCompositeId(Long part1, Long part2) {
			this.part1 = part1;
			this.part2 = part2;
		}

		public Long getPart1() {
			return part1;
		}

		public Long getPart2() {
			return part2;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			EmbeddedCompositeId that = (EmbeddedCompositeId) o;
			return Objects.equals( part1, that.part1 ) && Objects.equals( part2, that.part2 );
		}

		@Override
		public int hashCode() {
			return Objects.hash( part1, part2 );
		}
	}

	@Entity(name = "ParentWithFinalOneToMany")
	public static class ParentWithFinalOneToMany {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		private final List<ChildOfParent> children = new ArrayList<>();

		protected ParentWithFinalOneToMany() {
		}

		public ParentWithFinalOneToMany(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<ChildOfParent> getChildren() {
			return children;
		}
	}

	@Entity(name = "ChildOfParent")
	public static class ChildOfParent {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne
		private ParentWithFinalOneToMany parent;

		protected ChildOfParent() {
		}

		public ChildOfParent(String name, ParentWithFinalOneToMany parent) {
			this.name = name;
			this.parent = parent;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public ParentWithFinalOneToMany getParent() {
			return parent;
		}
	}

	@Entity(name = "AuthorWithFinalManyToMany")
	public static class AuthorWithFinalManyToMany {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToMany(cascade = CascadeType.ALL)
		private final Set<BookWithFinalManyToMany> books = new HashSet<>();

		protected AuthorWithFinalManyToMany() {
		}

		public AuthorWithFinalManyToMany(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Set<BookWithFinalManyToMany> getBooks() {
			return books;
		}
	}

	@Entity(name = "BookWithFinalManyToMany")
	public static class BookWithFinalManyToMany {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		@ManyToMany(mappedBy = "books")
		private final Set<AuthorWithFinalManyToMany> authors = new HashSet<>();

		protected BookWithFinalManyToMany() {
		}

		public BookWithFinalManyToMany(String title) {
			this.title = title;
		}

		public Long getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}

		public Set<AuthorWithFinalManyToMany> getAuthors() {
			return authors;
		}
	}

	@Entity(name = "EntityWithFinalElementCollection")
	public static class EntityWithFinalElementCollection {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ElementCollection
		private final Set<String> tags = new HashSet<>();

		protected EntityWithFinalElementCollection() {
		}

		public EntityWithFinalElementCollection(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Set<String> getTags() {
			return tags;
		}
	}
}
