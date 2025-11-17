/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sorted;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.collection.IsIterableContainingInOrder;

import static org.hamcrest.CoreMatchers.is;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.hibernate.testing.hamcrest.InitializationCheckMatcher.isInitialized;
import static org.hibernate.testing.hamcrest.InitializationCheckMatcher.isNotInitialized;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 * @author Brett Meyer
 * @author Nathan Xu
 */
@DomainModel( annotatedClasses = SortTest.Search.class )
@SessionFactory
@SuppressWarnings( "unused" )
public class SortTest {

	private String[] searchResultsBeforeSorting = {
			"jboss.com",
			"hibernate.org",
			"HiA"
	};

	private Map<String, String> tokensBeforeSorting = new HashMap<String, String>()
	{
		{
			put( "jboss", "jboss" );
			put( "hibernate", "hibernate" );
			put( "HiA", "hia");
		}
	};

	@BeforeEach
	void setUp(SessionFactoryScope scope) {

		// ensure query plan cache won't interfere
		scope.getSessionFactory().getQueryEngine().getInterpretationCache().close();

		final Search search = new Search( "Hibernate" );
		search.searchResults.addAll( Arrays.asList( searchResultsBeforeSorting ) );
		search.searchResultsCaseInsensitive.addAll( Arrays.asList( searchResultsBeforeSorting ) );
		search.tokens.putAll( tokensBeforeSorting );
		search.tokensCaseInsensitive.putAll( tokensBeforeSorting );
		scope.inTransaction(
				session -> session.persist( search )
		);
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testSortedSetDefinitionInHbmXml(SessionFactoryScope scope) {
		final PersistentClass entityMapping = scope.getMetadataImplementor().getEntityBinding( Search.class.getName() );

		final Property sortedSetProperty = entityMapping.getProperty( "searchResults" );
		final Collection sortedSetMapping = assertTyping( Collection.class, sortedSetProperty.getValue() );
		assertTrue( "SortedSet mapping not interpreted as sortable", sortedSetMapping.isSorted() );
		assertNull( sortedSetMapping.getComparator() );

		final Property sortedMapProperty = entityMapping.getProperty( "tokens" );
		final Collection sortedMapMapping = assertTyping( Collection.class, sortedMapProperty.getValue() );
		assertTrue( "SortedMap mapping not interpreted as sortable", sortedMapMapping.isSorted() );
		assertNull( sortedSetMapping.getComparator() );

		final Property sortedSetCaseInsensitiveProperty = entityMapping.getProperty( "searchResultsCaseInsensitive" );
		final Collection sortedSetCaseInsensitiveMapping = assertTyping(
				Collection.class,
				sortedSetCaseInsensitiveProperty.getValue()
		);
		assertTrue( "SortedSet mapping not interpreted as sortable", sortedSetCaseInsensitiveMapping.isSorted() );
		assertTyping( StringCaseInsensitiveComparator.class, sortedSetCaseInsensitiveMapping.getComparator() );

		final Property sortedMapCaseInsensitiveProperty = entityMapping.getProperty( "tokensCaseInsensitive" );
		final Collection sortedMapCaseInsensitiveMapping = assertTyping(
				Collection.class,
				sortedMapCaseInsensitiveProperty.getValue()
		);
		assertTrue( "SortedMap mapping not interpreted as sortable", sortedMapCaseInsensitiveMapping.isSorted() );
		assertTyping( StringCaseInsensitiveComparator.class, sortedMapCaseInsensitiveMapping.getComparator() );
	}

	@Test
	void testSortByCriteria(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					final CriteriaQuery<Search> criteria = criteriaBuilder.createQuery( Search.class );
					final Root<Search> root = criteria.from( Search.class );
					criteria.select( root );

					final Search search = session.createQuery( criteria ).uniqueResult();

					assertThat( search.searchResults, isNotInitialized() );
					assertSearchResultsOrderedNaturally( search.searchResults );

					assertThat( search.searchResultsCaseInsensitive, isNotInitialized() );
					assertSearchResultsOrderedCaseInsensitively( search.searchResultsCaseInsensitive );

					assertThat( search.tokens, isNotInitialized() );
					assertTokensOrderedNaturally( search.tokens );

					assertThat( search.tokensCaseInsensitive, isNotInitialized() );
					assertTokensOrderedCaseInsensitively( search.tokensCaseInsensitive );
				}
		);
	}

	@Test
	void testSortByCriteriaWithFetchLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					final CriteriaQuery<Search> criteria = criteriaBuilder.createQuery( Search.class );
					final Root<Search> root = criteria.from( Search.class );
					root.fetch( "searchResults", JoinType.LEFT );
					root.fetch( "searchResultsCaseInsensitive", JoinType.LEFT );
					root.fetch( "tokens", JoinType.LEFT );
					root.fetch( "tokensCaseInsensitive", JoinType.LEFT );
					criteria.select( root );

					final Search search = session.createQuery( criteria ).uniqueResult();

					assertThat( search.searchResults, isInitialized() );
					assertSearchResultsOrderedNaturally( search.searchResults );

					assertThat( search.searchResultsCaseInsensitive, isInitialized() );
					assertSearchResultsOrderedCaseInsensitively( search.searchResultsCaseInsensitive );

					assertThat( search.tokens, isInitialized() );
					assertTokensOrderedNaturally( search.tokens );

					assertThat( search.tokensCaseInsensitive, isInitialized() );
					assertTokensOrderedCaseInsensitively( search.tokensCaseInsensitive );
				}
		);
	}

	@Test
	void testSortByHQL(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Search search = (Search) session.createQuery(
							"select s from Search s " +
									" join fetch s.searchResults " +
									" join fetch s.searchResultsCaseInsensitive " +
									" join fetch s.tokens " +
									" join fetch s.tokensCaseInsensitive " )
							.uniqueResult();

					assertThat( search.searchResults, isInitialized() );
					assertSearchResultsOrderedNaturally( search.searchResults );

					assertThat( search.searchResultsCaseInsensitive, isInitialized() );
					assertSearchResultsOrderedCaseInsensitively( search.searchResultsCaseInsensitive );

					assertThat( search.tokens, isInitialized() );
					assertTokensOrderedNaturally( search.tokens );

					assertThat( search.tokensCaseInsensitive, isInitialized() );
					assertTokensOrderedCaseInsensitively( search.tokensCaseInsensitive );
				}
		);
	}

	private void assertSearchResultsOrderedNaturally(SortedSet<String> orderedSearchResults) {
		assertThat( orderedSearchResults, IsIterableContainingInOrder.contains(
				Arrays.stream( searchResultsBeforeSorting ).sorted().toArray()
		) );
	}

	private void assertSearchResultsOrderedCaseInsensitively(SortedSet<String> orderedSearchResults) {
		assertThat( orderedSearchResults, IsIterableContainingInOrder.contains(
				Arrays.stream( searchResultsBeforeSorting ).sorted( String.CASE_INSENSITIVE_ORDER).toArray()
		) );
	}

	private void assertTokensOrderedNaturally(SortedMap<String, String> orderedTokens) {
		assertTokensOrdered( orderedTokens,  new TreeMap<>( tokensBeforeSorting ) );
	}

	private void assertTokensOrderedCaseInsensitively(SortedMap<String, String> orderedTokens) {
		final TreeMap<String, String> expectedOrderedTokens = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		expectedOrderedTokens.putAll( tokensBeforeSorting );
		assertTokensOrdered( orderedTokens, expectedOrderedTokens );
	}

	private void assertTokensOrdered(SortedMap<String, String> actualOrderedTokens, TreeMap<String, String> expectedOrderedTokens) {
		assertThat( actualOrderedTokens.entrySet(), hasSize( expectedOrderedTokens.entrySet().size() ) );
		final Iterator<Map.Entry<String, String>> actualIterator = actualOrderedTokens.entrySet().iterator();
		final Iterator<Map.Entry<String, String>> expectedIterator = expectedOrderedTokens.entrySet().iterator();
		while ( actualIterator.hasNext() ) {
			Map.Entry<String, String> actualEntry = actualIterator.next();
			Map.Entry<String, String> expectedEntry = expectedIterator.next();
			assertThat( actualEntry.getKey(), is( expectedEntry.getKey() ) );
			assertThat( actualEntry.getValue(), is( expectedEntry.getValue() ) );
		}
	}

	@Entity(name = "Search")
	@Table(name = "Search")
	public static class Search {

		@Id
		private String searchString;

		@ElementCollection
		@SortNatural
		@CollectionTable( name = "Search_results1")
		private SortedSet<String> searchResults = new TreeSet<>();

		@ElementCollection
		@SortComparator(StringCaseInsensitiveComparator.class)
		@CollectionTable( name = "Search_results2")
		private SortedSet<String> searchResultsCaseInsensitive = new TreeSet<>();

		@ElementCollection
		@SortNatural
		@CollectionTable( name = "Search_tokens1")
		private SortedMap<String, String> tokens = new TreeMap<>();

		@ElementCollection
		@SortComparator(StringCaseInsensitiveComparator.class)
		@CollectionTable( name = "Search_tokens2")
		private SortedMap<String, String> tokensCaseInsensitive = new TreeMap<>();

		public Search() {
		}

		public Search(String searchString) {
			this.searchString = searchString;
		}
	}

}
