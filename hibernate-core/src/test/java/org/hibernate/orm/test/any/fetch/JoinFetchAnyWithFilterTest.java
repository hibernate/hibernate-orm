/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.fetch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import org.hibernate.EntityFilterException;
import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.Hibernate.isInitialized;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory(useCollectingStatementInspector = true)
@DomainModel(annotatedClasses = {
		JoinFetchAnyWithFilterTest.AnyThing.class,
		JoinFetchAnyWithFilterTest.FilteredThing.class,
		JoinFetchAnyWithFilterTest.UnfilteredThing.class
})
class JoinFetchAnyWithFilterTest {

	@BeforeEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testJoinFetchWithFilterEnabled(SessionFactoryScope scope) {
		// Setup: Create an AnyThing pointing to a FilteredThing with active=false
		scope.inTransaction( s -> {
			var anyThing = new AnyThing();
			var filteredThing = new FilteredThing();
			filteredThing.description = "Filtered thing";
			filteredThing.active = false; // This will be filtered out when filter is enabled
			anyThing.anything = filteredThing;
			s.persist( filteredThing );
			s.persist( anyThing );
		} );

		// Without filter: should work normally
		scope.inTransaction( s -> {
			var result = s.createQuery( "from AnyThing a join fetch a.anything", AnyThing.class )
					.getSingleResult();
			assertTrue( isInitialized( result.anything ) );
			assertInstanceOf( FilteredThing.class, result.anything );
			assertEquals( "Filtered thing", ((FilteredThing) result.anything).description );
			assertFalse( ((FilteredThing) result.anything).active );
		} );

		// With filter enabled: should throw EntityFilterException
		assertThrows( EntityFilterException.class, () -> {
			scope.inTransaction( s -> {
				s.enableFilter( "activeFilter" ).setParameter( "active", true );
				s.createQuery( "from AnyThing a join fetch a.anything", AnyThing.class )
						.getSingleResult();
			} );
		} );
	}

	@Test
	void testJoinFetchWithFilterEnabledButMatching(SessionFactoryScope scope) {
		// Setup: Create an AnyThing pointing to a FilteredThing with active=true
		scope.inTransaction( s -> {
			var anyThing = new AnyThing();
			var filteredThing = new FilteredThing();
			filteredThing.description = "Active filtered thing";
			filteredThing.active = true; // This will NOT be filtered out
			anyThing.anything = filteredThing;
			s.persist( filteredThing );
			s.persist( anyThing );
		} );

		// With filter enabled but entity matches: should work normally
		scope.inTransaction( s -> {
			s.enableFilter( "activeFilter" ).setParameter( "active", true );
			var result = s.createQuery( "from AnyThing a join fetch a.anything", AnyThing.class )
					.getSingleResult();
			assertTrue( isInitialized( result.anything ) );
			assertInstanceOf( FilteredThing.class, result.anything );
			assertEquals( "Active filtered thing", result.anything.getDescription() );
			assertTrue( ((FilteredThing) result.anything).isActive() );
		} );
	}

	@Test
	void testJoinFetchPolymorphicWithFilterEnabled(SessionFactoryScope scope) {
		// Setup: Create multiple AnyThings with different target types
		scope.inTransaction( s -> {
			// Filtered thing that will be filtered out
			var anyThing1 = new AnyThing();
			var filteredThing1 = new FilteredThing();
			filteredThing1.description = "Inactive filtered";
			filteredThing1.active = false;
			anyThing1.anything = filteredThing1;
			s.persist( filteredThing1 );
			s.persist( anyThing1 );

			// Filtered thing that matches filter
			var anyThing2 = new AnyThing();
			var filteredThing2 = new FilteredThing();
			filteredThing2.description = "Active filtered";
			filteredThing2.active = true;
			anyThing2.anything = filteredThing2;
			s.persist( filteredThing2 );
			s.persist( anyThing2 );

			// Unfiltered thing
			var anyThing3 = new AnyThing();
			var unfilteredThing = new UnfilteredThing();
			unfilteredThing.description = "Unfiltered";
			anyThing3.anything = unfilteredThing;
			s.persist( unfilteredThing );
			s.persist( anyThing3 );
		} );

		// Without filter: all three should load
		scope.inTransaction( s -> {
			var results = s.createQuery( "from AnyThing a join fetch a.anything order by a.id", AnyThing.class )
					.getResultList();
			assertEquals( 3, results.size() );
			assertTrue( isInitialized( results.get( 0 ).anything ) );
			assertTrue( isInitialized( results.get( 1 ).anything ) );
			assertTrue( isInitialized( results.get( 2 ).anything ) );
		} );

		// With filter enabled: should throw EntityFilterException for the first one
		assertThrows( EntityFilterException.class, () -> {
			scope.inTransaction( s -> {
				s.enableFilter( "activeFilter" ).setParameter( "active", true );
				s.createQuery( "from AnyThing a join fetch a.anything order by a.id", AnyThing.class )
						.getResultList();
			} );
		} );
	}

	@Test
	void testEntityGraphWithFilterEnabled(SessionFactoryScope scope) {
		// Setup
		scope.inTransaction( s -> {
			var anyThing = new AnyThing();
			var filteredThing = new FilteredThing();
			filteredThing.description = "Filtered thing";
			filteredThing.active = false;
			anyThing.anything = filteredThing;
			s.persist( filteredThing );
			s.persist( anyThing );
		} );

		// Entity graph with filter enabled should also throw EntityFilterException
		assertThrows( EntityFilterException.class, () -> {
			scope.inTransaction( s -> {
				s.enableFilter( "activeFilter" ).setParameter( "active", true );
				var graph = s.createEntityGraph( AnyThing.class );
				graph.addAttributeNode( JoinFetchAnyWithFilterTest_.AnyThing_.anything );
				s.find( graph, 1L );
			} );
		} );
	}

	@Test
	void testLazyLoadWithFilterEnabledButMatching(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var anyThing = new AnyThing();
			var filteredThing = new FilteredThing();
			filteredThing.description = "Active filtered thing";
			filteredThing.active = true;
			anyThing.anything = filteredThing;
			s.persist( filteredThing );
			s.persist( anyThing );
		} );

		// With filter enabled but entity matches: should work normally
		scope.inTransaction( s -> {
			s.enableFilter( "activeFilter" ).setParameter( "active", true );
			var result = s.createQuery( "from AnyThing a", AnyThing.class ).getSingleResult();
			assertFalse( isInitialized( result.anything ) );
			Hibernate.initialize( result.anything );
			assertInstanceOf( FilteredThing.class, Hibernate.unproxy( result.anything ) );
			assertEquals( "Active filtered thing", result.anything.getDescription() );
			assertTrue( result.anything.isActive() );
		} );
	}

	@Test
	void testLazyLoadWithFilterEnabled(SessionFactoryScope scope) {
		// Setup
		scope.inTransaction( s -> {
			var anyThing = new AnyThing();
			var filteredThing = new FilteredThing();
			filteredThing.description = "Filtered thing";
			filteredThing.active = false;
			anyThing.anything = filteredThing;
			s.persist( filteredThing );
			s.persist( anyThing );
		} );

		// Lazy load without join fetch - the filter affects initialization
		scope.inTransaction( s -> {
			s.enableFilter( "activeFilter" ).setParameter( "active", true );
			var result = s.createQuery( "from AnyThing a", AnyThing.class )
					.getSingleResult();
			assertNotNull( result );
			assertFalse( isInitialized( result.anything ) );

			// Lazy loading of a proxy will throw ObjectNotFoundException instead of EntityFilterException,
			// even if the object wasn't found due to the use of a filter
			assertThrows( ObjectNotFoundException.class, () -> {
				result.anything.getDescription();
			} );
		} );
	}

	@Entity(name = "AnyThing")
	static class AnyThing {
		@Id
		@GeneratedValue
		Long id;

		@Any(fetch = FetchType.LAZY)
		@AnyKeyJavaClass(Long.class)
		@JoinColumn(name = "ANYTHING_ID")
		@Column(name = "ANYTHING_TYPE")
		@AnyDiscriminatorValue(
				discriminator = "FilteredThing",
				entity = FilteredThing.class)
		@AnyDiscriminatorValue(
				discriminator = "UnfilteredThing",
				entity = UnfilteredThing.class)
		Thing anything;
	}

	static class Thing {
		public String getDescription() {
			return null;
		}
		public boolean isActive() {
			return true;
		}
	}

	@Entity(name = "FilteredThing")
	@FilterDef(name = "activeFilter", parameters = @ParamDef(name = "active", type = Boolean.class), applyToLoadByKey = true)
	@Filter(name = "activeFilter", condition = "active = :active")
	static class FilteredThing extends Thing {
		@Id
		@GeneratedValue
		Long id;

		String description;

		boolean active;

		@Override
		public String getDescription() {
			return description;
		}

		public boolean isActive() {
			return active;
		}
	}

	@Entity(name = "UnfilteredThing")
	static class UnfilteredThing extends Thing {
		@Id
		@GeneratedValue
		Long id;

		String description;

		@Override
		public String getDescription() {
			return description;
		}
	}
}
