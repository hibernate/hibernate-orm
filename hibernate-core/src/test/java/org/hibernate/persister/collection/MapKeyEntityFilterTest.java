package org.hibernate.persister.collection;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyJoinColumn;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.annotations.FetchMode.JOIN;
import static org.hibernate.graph.GraphSemantic.FETCH;
import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { MapKeyEntityFilterTest.Owner.class, MapKeyEntityFilterTest.Note.class,
		MapKeyEntityFilterTest.Tag.class })
@SessionFactory
@JiraKey("HHH-21216")
class MapKeyEntityFilterTest {
	enum FetchPath { EAGER_FIND, LAZY, HQL_FETCH, FETCH_GRAPH }

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = new Owner();
			owner.id = 1;
			final var tag = new Tag();
			tag.id = 1;
			session.persist( tag );
			for ( int id : new int[] { 10, 20 } ) {
				final var note = new Note();
				note.id = id;
				session.persist( note );
				owner.tags.put( note, tag );
			}
			session.persist( owner );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@EnumSource(FetchPath.class)
	void ordinaryKeyFilter(FetchPath path, SessionFactoryScope scope) {
		assertKeys( scope, path, new Integer[] { 10, 20 }, "queryNotes" );
	}

	@ParameterizedTest
	@EnumSource(FetchPath.class)
	void loadByKeyFilter(FetchPath path, SessionFactoryScope scope) {
		assertKeys( scope, path, new Integer[] { 10 }, "loadNotes" );
	}

	@ParameterizedTest
	@EnumSource(FetchPath.class)
	void bothKeyFilters(FetchPath path, SessionFactoryScope scope) {
		assertKeys( scope, path, new Integer[] { 10 }, "queryNotes", "loadNotes" );
	}

	private void assertKeys(SessionFactoryScope scope, FetchPath path, Integer[] expected, String... filters) {
		scope.inSession( session -> {
			for ( String filter : filters ) {
				session.enableFilter( filter );
			}
			final Owner owner = switch ( path ) {
				case EAGER_FIND -> session.find( Owner.class, 1 );
				case LAZY -> session.find( session.createEntityGraph( Owner.class ), 1, FETCH );
				case HQL_FETCH -> session.createSelectionQuery(
						"from MapOwner o left join fetch o.tags where o.id = 1", Owner.class ).getSingleResult();
				case FETCH_GRAPH -> {
					final var graph = session.createEntityGraph( Owner.class );
					graph.addAttributeNodes( "tags" );
					yield session.find( graph, 1, FETCH );
				}
			};
			assertThat( Hibernate.isInitialized( owner.tags ) ).isEqualTo( path != FetchPath.LAZY );
			assertThat( owner.tags.keySet() ).extracting( note -> note.id ).containsExactlyInAnyOrder( expected );
			assertThat( owner.tags.values() ).allSatisfy( tag -> assertThat( tag.id ).isEqualTo( 1 ) );
		} );
	}

	@Test
	void uninitializedMapReads(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.enableFilter( "queryNotes" );
			final var tags = session.find( session.createEntityGraph( Owner.class ), 1, FETCH ).tags;
			final var key = session.getReference( Note.class, 20 );
			assertThat( ( (PersistentCollection<?>) tags ).getSize() ).isEqualTo( 2 );
			assertThat( Hibernate.get( tags, key ) ).isNotNull();
			assertThat( Hibernate.isInitialized( tags ) ).isFalse();
		} );
		scope.inSession( session -> {
			session.enableFilter( "queryNotes" );
			session.enableFilter( "loadNotes" );
			final var tags = session.find( session.createEntityGraph( Owner.class ), 1, FETCH ).tags;
			final var key = session.getReference( Note.class, 20 );
			assertThat( ( (PersistentCollection<?>) tags ).getSize() ).isEqualTo( 1 );
			assertThat( Hibernate.get( tags, key ) ).isNull();
			assertThat( Hibernate.isInitialized( tags ) ).isFalse();
		} );
	}

	@Test
	void collectionLoaderReuse(SessionFactoryScope scope) {
		final var persister = (AbstractCollectionPersister) scope.getSessionFactory().getMappingMetamodel()
				.getCollectionDescriptor( Owner.class.getName() + ".tags" );
		scope.inSession( session -> {
			session.enableFilter( "queryTags" );
			assertThat( persister.determineLoaderToUse( 1, session ) ).isSameAs( persister.getCollectionLoader() );
			session.enableFilter( "queryNotes" );
			assertThat( persister.determineLoaderToUse( 1, session ) ).isSameAs( persister.getCollectionLoader() );
			session.enableFilter( "loadNotes" );
			assertThat( persister.determineLoaderToUse( 1, session ) ).isNotSameAs( persister.getCollectionLoader() );
		} );
	}

	@Entity(name = "MapOwner")
	static class Owner {
		@Id int id;
		@ManyToMany(fetch = EAGER)
		@Fetch(JOIN)
		@MapKeyJoinColumn(name = "note_id")
		Map<Note, Tag> tags = new HashMap<>();
	}

	@Entity(name = "Note")
	@FilterDef(name = "queryNotes")
	@Filter(name = "queryNotes", condition = "id <> 10")
	@FilterDef(name = "loadNotes", applyToLoadByKey = true)
	@Filter(name = "loadNotes", condition = "id <> 20")
	static class Note {
		@Id int id;
	}

	@Entity(name = "Tag")
	@FilterDef(name = "queryTags")
	@Filter(name = "queryTags", condition = "id <> 1")
	static class Tag {
		@Id int id;
	}
}
