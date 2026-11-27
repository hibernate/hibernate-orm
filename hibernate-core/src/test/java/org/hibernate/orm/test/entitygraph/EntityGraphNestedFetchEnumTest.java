package org.hibernate.orm.test.entitygraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumeratedValue;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.FetchMethod;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks {@link FetchMethod#NESTED} with enums, including {@link EnumeratedValue},
 * comparing with {@link FetchMethod#BY_ID}.
 */
@DomainModel(
		annotatedClasses = {
				EntityGraphNestedFetchEnumTest.Wardrobe.class,
				EntityGraphNestedFetchEnumTest.Shirt.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNestedCollectionFetch.class)
@JiraKey("HHH-9558")
class EntityGraphNestedFetchEnumTest {

	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var wardrobe = new Wardrobe();
			wardrobe.id = 1L;
			wardrobe.colors.addAll( Set.of( Color.RED, Color.BLUE ) );
			wardrobe.colorOrdinals.addAll( Set.of( Color.GREEN, Color.BLUE ) );
			wardrobe.sizes.addAll( Set.of( Size.SMALL, Size.LARGE ) );
			wardrobe.statuses.addAll( Set.of( Status.ACTIVE, Status.RETIRED ) );
			session.persist( wardrobe );
			final Size[] sizes = { Size.LARGE, Size.MEDIUM, null };
			final Status[] statuses = { Status.RETIRED, null, Status.ACTIVE };
			for ( int i = 0; i < 3; i++ ) {
				final var shirt = new Shirt();
				shirt.id = i + 1L;
				shirt.size = sizes[i];
				shirt.status = statuses[i];
				shirt.wardrobe = wardrobe;
				session.persist( shirt );
			}
		} );
		scope.getCollectingStatementInspector().clear();
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	private static void assertFetchedBy(FetchMethod method, SessionFactoryScope scope) {
		final boolean nested = scope.getCollectingStatementInspector().getSqlQueries().stream()
				.anyMatch( sql -> sql.contains( "json" ) );
		assertThat( nested ).isEqualTo( method == FetchMethod.NESTED );
	}

	@Test
	void enumeratedValuesOfElements_BY_ID(SessionFactoryScope scope) {
		enumeratedValuesOfElements( FetchMethod.BY_ID, scope );
	}

	@Test
	void enumeratedValuesOfElements_NESTED(SessionFactoryScope scope) {
		enumeratedValuesOfElements( FetchMethod.NESTED, scope );
	}

	private void enumeratedValuesOfElements(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Wardrobe.class );
			graph.addAttributeNode( "shirts" ).addOption( method );
			final var wardrobe = session.find( graph, 1L );
			assertFetchedBy( method, scope );
			final var shirts = wardrobe.shirts.stream().sorted( Comparator.comparing( s -> s.id ) ).toList();
			assertThat( shirts ).extracting( s -> s.size ).containsExactly( Size.LARGE, Size.MEDIUM, null );
			assertThat( shirts ).extracting( s -> s.status ).containsExactly( Status.RETIRED, null, Status.ACTIVE );
		} );
	}

	@Test
	void enumElementCollections_BY_ID(SessionFactoryScope scope) {
		enumElementCollections( FetchMethod.BY_ID, scope );
	}

	@Test
	void enumElementCollections_NESTED(SessionFactoryScope scope) {
		enumElementCollections( FetchMethod.NESTED, scope );
	}

	private void enumElementCollections(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Wardrobe.class );
			for ( var attribute : List.of( "colors", "colorOrdinals", "sizes", "statuses" ) ) {
				graph.addAttributeNode( attribute ).addOption( method );
			}
			final var wardrobe = session.find( graph, 1L );
			assertFetchedBy( method, scope );
			assertThat( wardrobe.colors ).containsExactlyInAnyOrder( Color.RED, Color.BLUE );
			assertThat( wardrobe.colorOrdinals ).containsExactlyInAnyOrder( Color.GREEN, Color.BLUE );
			assertThat( wardrobe.sizes ).containsExactlyInAnyOrder( Size.SMALL, Size.LARGE );
			assertThat( wardrobe.statuses ).containsExactlyInAnyOrder( Status.ACTIVE, Status.RETIRED );
		} );
	}

	enum Color { RED, GREEN, BLUE }

	/**
	 * Stored as a string code which is neither the name nor the ordinal.
	 */
	enum Size {
		SMALL( "S" ),
		MEDIUM( "M" ),
		LARGE( "L" );

		@EnumeratedValue
		private final String code;

		Size(String code) {
			this.code = code;
		}
	}

	/**
	 * Stored as an integer code which is not the ordinal.
	 */
	enum Status {
		DRAFT( 10 ),
		ACTIVE( 20 ),
		RETIRED( 30 );

		@EnumeratedValue
		private final int code;

		Status(int code) {
			this.code = code;
		}
	}

	@Entity(name = "NfeWardrobe")
	@Table(name = "nfen_wardrobe")
	static class Wardrobe {
		@Id
		Long id;
		@OneToMany(mappedBy = "wardrobe")
		List<Shirt> shirts = new ArrayList<>();
		@ElementCollection
		@Enumerated(EnumType.STRING)
		Set<Color> colors = new HashSet<>();
		@ElementCollection
		@Enumerated(EnumType.ORDINAL)
		Set<Color> colorOrdinals = new HashSet<>();
		@ElementCollection
		@Enumerated(EnumType.STRING)
		Set<Size> sizes = new HashSet<>();
		@ElementCollection
		@Enumerated(EnumType.ORDINAL)
		Set<Status> statuses = new HashSet<>();
	}

	@Entity(name = "NfeShirt")
	@Table(name = "nfen_shirt")
	static class Shirt {
		@Id
		Long id;
		@Enumerated(EnumType.STRING)
		Size size;
		@Enumerated(EnumType.ORDINAL)
		Status status;
		@ManyToOne(fetch = FetchType.LAZY)
		Wardrobe wardrobe;
	}
}
