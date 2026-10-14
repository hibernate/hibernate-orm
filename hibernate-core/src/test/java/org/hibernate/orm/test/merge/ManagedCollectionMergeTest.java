package org.hibernate.orm.test.merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.hibernate.annotations.CollectionType;
import org.hibernate.orm.test.mapping.collections.semantics.UniqueListType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = ManagedCollectionMergeTest.Holder.class)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20973")
class ManagedCollectionMergeTest {
	static Stream<Arguments> collectionsAndFlush() {
		return Arrays.stream( Kind.values() ).flatMap( kind -> Stream.of(
				Arguments.of( kind, false ), Arguments.of( kind, true )
		) );
	}

	@ParameterizedTest
	@MethodSource("collectionsAndFlush")
	void unchangedCollectionDoesNotIncrementVersion(Kind kind, boolean flushFirst, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var holder = new Holder();
			kind.populate( holder );
			session.persist( holder );
			if ( flushFirst ) {
				session.flush();
			}
			var collection = kind.get( holder );
			long version = holder.version;
			assertThat( session.merge( holder ) ).isSameAs( holder );
			assertThat( kind.get( holder ) ).isSameAs( collection );
			session.flush();
			assertThat( holder.version ).isEqualTo( version );
		} );
	}

	@ParameterizedTest
	@EnumSource(Kind.class)
	void changesThroughBackingCollectionSurviveMerge(Kind kind, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var holder = new Holder();
			kind.populate( holder );
			var backing = kind.get( holder );
			session.persist( holder );
			session.flush();
			kind.change( backing );
			session.merge( holder );
			session.flush();
			session.flush();
		} );
		scope.inTransaction( session -> {
			var value = kind.get( session.find( Holder.class, 1L ) );
			if ( value instanceof String[] array ) {
				assertThat( array ).containsExactly( "changed", "a" );
			}
			else if ( value instanceof Map<?, ?> map ) {
				assertThat( map ).isEmpty();
			}
			else {
				assertThat( (Collection<?>) value ).isEmpty();
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	enum Kind {
		BAG, LIST, SET, SORTED_SET, MAP, SORTED_MAP, ARRAY, CUSTOM;

		Object get(Holder holder) {
			return switch ( this ) {
				case BAG -> holder.bag;
				case LIST -> holder.list;
				case SET -> holder.set;
				case SORTED_SET -> holder.sortedSet;
				case MAP -> holder.map;
				case SORTED_MAP -> holder.sortedMap;
				case ARRAY -> holder.array;
				case CUSTOM -> holder.custom;
			};
		}

		@SuppressWarnings("unchecked")
		void populate(Holder holder) {
			var value = get( holder );
			if ( value instanceof String[] ) {
				holder.array = new String[] { "b", "a" };
			}
			else if ( value instanceof Map<?, ?> ) {
				var map = (Map<String, String>) value;
				map.put( "b", "b" );
				map.put( "a", "a" );
			}
			else {
				((Collection<String>) value).addAll( List.of( "b", "a" ) );
			}
		}

		void change(Object value) {
			if ( value instanceof String[] array ) {
				array[0] = "changed";
			}
			else if ( value instanceof Map<?, ?> map ) {
				map.clear();
			}
			else {
				((Collection<?>) value).clear();
			}
		}
	}

	@Entity(name = "MergeCollectionHolder")
	static class Holder {
		@Id
		long id = 1;
		@Version
		long version;
		@ElementCollection
		@Column(name = "element_value")
		List<String> bag = new ArrayList<>();
		@ElementCollection
		@OrderColumn(name = "element_position")
		@Column(name = "element_value")
		List<String> list = new ArrayList<>();
		@ElementCollection
		@Column(name = "element_value")
		Set<String> set = new LinkedHashSet<>();
		@ElementCollection
		@Column(name = "element_value")
		SortedSet<String> sortedSet = new TreeSet<>();
		@ElementCollection
		@MapKeyColumn(name = "element_key")
		@Column(name = "element_value")
		Map<String, String> map = new LinkedHashMap<>();
		@ElementCollection
		@MapKeyColumn(name = "element_key")
		@Column(name = "element_value")
		SortedMap<String, String> sortedMap = new TreeMap<>();
		@ElementCollection
		@OrderColumn(name = "element_position")
		@Column(name = "element_value")
		String[] array = new String[0];
		@ElementCollection
		@CollectionType(type = UniqueListType.class)
		@Column(name = "element_value")
		List<String> custom = new ArrayList<>();
	}
}
