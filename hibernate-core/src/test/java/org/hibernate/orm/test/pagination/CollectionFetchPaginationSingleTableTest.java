package org.hibernate.orm.test.pagination;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Formula;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression based on the reproducer by Simon Juresa.
 */
@DomainModel(annotatedClasses = {
		CollectionFetchPaginationSingleTableTest.Item.class,
		CollectionFetchPaginationSingleTableTest.Video.class,
		CollectionFetchPaginationSingleTableTest.PremiumVideo.class,
		CollectionFetchPaginationSingleTableTest.Source.class,
		CollectionFetchPaginationSingleTableTest.Tag.class
})
@ServiceRegistry(settings = {
		@Setting(name = QuerySettings.QUERY_PLAN_CACHE_ENABLED, value = "false"),
		@Setting(name = QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH, value = "true")
})
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOffsetInSubquery.class)
@JiraKey("HHH-20942")
class CollectionFetchPaginationSingleTableTest {
	private static final String FETCH_QUERY =
			"select i from Item i join fetch i.source s left join fetch s.tags";

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Source source = new Source();
			source.id = 1L;
			session.persist( source );
			for ( long id = 1; id <= 2; id++ ) {
				final Tag tag = new Tag();
				tag.id = id;
				tag.source = source;
				source.tags.add( tag );
				session.persist( tag );
			}
			for ( long id = 1; id <= 4; id++ ) {
				final Item item;
				if ( id <= 3 ) {
					final Video video;
					if ( id == 2 ) {
						final PremiumVideo premium = new PremiumVideo();
						premium.quality = "high";
						video = premium;
					}
					else {
						video = new Video();
					}
					video.channelId = "channel-" + id;
					item = video;
				}
				else {
					item = new Item();
				}
				item.id = id;
				item.source = source;
				session.persist( item );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void paginatedRootIncludesSubclassColumns(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Item> items = session.createSelectionQuery( FETCH_QUERY + " order by i.id", Item.class )
					.setMaxResults( 2 )
					.getResultList();
			assertItems( items, 1L, 2L );
		} );
	}

	@Test
	void offsetPageIncludesPlainRoot(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertItems(
				session.createSelectionQuery( FETCH_QUERY + " order by i.id", Item.class )
						.setFirstResult( 2 ).setMaxResults( 2 ).getResultList(),
				3L, 4L
		) );
	}

	@Test
	void limitOneLookupIncludesSubclassColumns(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertItems(
				session.createSelectionQuery( FETCH_QUERY + " where i.id = :id", Item.class )
						.setParameter( "id", 2L ).setMaxResults( 1 ).getResultList(),
				2L
		) );
	}

	@Test
	void subclassRootIncludesDescendantColumns(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertItems(
				session.createSelectionQuery(
						"select v from Video v join fetch v.source s left join fetch s.tags order by v.id",
						Video.class
				).setFirstResult( 1 ).setMaxResults( 2 ).getResultList(),
				2L, 3L
		) );
	}

	@Test
	void leafLookupPreservesInheritedState(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertItems(
				session.createSelectionQuery(
						"select v from PremiumVideo v join fetch v.source s left join fetch s.tags where v.id = :id",
						PremiumVideo.class
				).setParameter( "id", 2L ).setMaxResults( 1 ).getResultList(),
				2L
		) );
	}

	@Test
	void unlimitedQueryPreservesHierarchyState(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertItems(
				session.createSelectionQuery( FETCH_QUERY + " order by i.id", Item.class ).getResultList(),
				1L, 2L, 3L, 4L
		) );
	}

	@Test
	void inMemoryLimitPreservesHierarchyState(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertItems(
				session.createSelectionQuery( FETCH_QUERY + " order by i.id", Item.class )
						.setHint( HibernateHints.HINT_LIMIT_IN_MEMORY, true )
						.setFirstResult( 1 ).setMaxResults( 2 ).getResultList(),
				2L, 3L
		) );
	}

	private static void assertItems(List<? extends Item> items, Long... ids) {
		assertEquals( List.of( ids ), items.stream().map( item -> item.id ).toList() );
		for ( Item item : items ) {
			assertEquals( 0, item.version );
			if ( item.id <= 3 ) {
				final Video video = assertInstanceOf( Video.class, item );
				assertEquals( "channel-" + item.id, video.channelId );
				assertEquals( "CHANNEL-" + item.id, video.upperChannelId );
				if ( item.id == 2 ) {
					assertEquals( "high", assertInstanceOf( PremiumVideo.class, item ).quality );
				}
			}
			else {
				assertEquals( Item.class, item.getClass() );
			}
			assertTrue( Hibernate.isInitialized( item.source ) );
			assertTrue( Hibernate.isInitialized( item.source.tags ) );
			assertEquals( Set.of( 1L, 2L ), item.source.tags.stream().map( tag -> tag.id ).collect( toSet() ) );
		}
	}

	@Entity(name = "Item")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	static class Item {
		@Id
		Long id;

		@Version
		int version;

		@ManyToOne(optional = false)
		Source source;
	}

	@Entity(name = "Video")
	@DiscriminatorValue("Video")
	static class Video extends Item {
		@Column(name = "channel_id")
		String channelId;

		@Formula("upper(channel_id)")
		String upperChannelId;
	}

	@Entity(name = "PremiumVideo")
	@DiscriminatorValue("PremiumVideo")
	static class PremiumVideo extends Video {
		String quality;
	}

	@Entity(name = "Source")
	static class Source {
		@Id
		Long id;

		@OneToMany(mappedBy = "source")
		Set<Tag> tags = new HashSet<>();
	}

	@Entity(name = "Tag")
	static class Tag {
		@Id
		Long id;

		@ManyToOne
		Source source;
	}
}
