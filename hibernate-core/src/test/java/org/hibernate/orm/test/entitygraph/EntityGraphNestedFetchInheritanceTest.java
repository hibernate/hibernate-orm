package org.hibernate.orm.test.entitygraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
 * Checks {@link FetchMethod#NESTED} with inheritance, comparing with {@link FetchMethod#BY_ID}:
 * owners and elements in {@code SINGLE_TABLE}, {@code JOINED} and {@code TABLE_PER_CLASS}
 * hierarchies, and mapped superclasses. A collection of elements of an entity hierarchy is
 * fetched using a secondary select instead.
 */
@DomainModel(
		annotatedClasses = {
				EntityGraphNestedFetchInheritanceTest.SinglePerson.class,
				EntityGraphNestedFetchInheritanceTest.SingleAnimal.class,
				EntityGraphNestedFetchInheritanceTest.SingleDog.class,
				EntityGraphNestedFetchInheritanceTest.SingleCat.class,
				EntityGraphNestedFetchInheritanceTest.SingleToy.class,
				EntityGraphNestedFetchInheritanceTest.JoinedPerson.class,
				EntityGraphNestedFetchInheritanceTest.JoinedAnimal.class,
				EntityGraphNestedFetchInheritanceTest.JoinedDog.class,
				EntityGraphNestedFetchInheritanceTest.JoinedCat.class,
				EntityGraphNestedFetchInheritanceTest.JoinedToy.class,
				EntityGraphNestedFetchInheritanceTest.UnionPerson.class,
				EntityGraphNestedFetchInheritanceTest.UnionAnimal.class,
				EntityGraphNestedFetchInheritanceTest.UnionDog.class,
				EntityGraphNestedFetchInheritanceTest.UnionCat.class,
				EntityGraphNestedFetchInheritanceTest.UnionToy.class,
				EntityGraphNestedFetchInheritanceTest.Article.class,
				EntityGraphNestedFetchInheritanceTest.Paragraph.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNestedCollectionFetch.class)
@JiraKey("HHH-9558")
class EntityGraphNestedFetchInheritanceTest {

	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var singlePerson = new SinglePerson();
			singlePerson.id = 1L;
			session.persist( singlePerson );
			final var singleDog = new SingleDog();
			singleDog.id = 1L;
			singleDog.name = "Rex";
			singleDog.goodBoy = true;
			singleDog.owner = singlePerson;
			session.persist( singleDog );
			final var singleCat = new SingleCat();
			singleCat.id = 2L;
			singleCat.name = "Tom";
			singleCat.lives = 9;
			singleCat.owner = singlePerson;
			session.persist( singleCat );
			long singleToyId = 1;
			for ( var label : List.of( "ball", "bone", "mouse" ) ) {
				final var toy = new SingleToy();
				toy.id = singleToyId++;
				toy.label = label;
				toy.animal = label.equals( "mouse" ) ? singleCat : singleDog;
				session.persist( toy );
			}
			final var joinedPerson = new JoinedPerson();
			joinedPerson.id = 1L;
			session.persist( joinedPerson );
			final var joinedDog = new JoinedDog();
			joinedDog.id = 1L;
			joinedDog.name = "Rex";
			joinedDog.goodBoy = true;
			joinedDog.owner = joinedPerson;
			session.persist( joinedDog );
			final var joinedCat = new JoinedCat();
			joinedCat.id = 2L;
			joinedCat.name = "Tom";
			joinedCat.lives = 9;
			joinedCat.owner = joinedPerson;
			session.persist( joinedCat );
			long joinedToyId = 1;
			for ( var label : List.of( "ball", "bone", "mouse" ) ) {
				final var toy = new JoinedToy();
				toy.id = joinedToyId++;
				toy.label = label;
				toy.animal = label.equals( "mouse" ) ? joinedCat : joinedDog;
				session.persist( toy );
			}
			final var unionPerson = new UnionPerson();
			unionPerson.id = 1L;
			session.persist( unionPerson );
			final var unionDog = new UnionDog();
			unionDog.id = 1L;
			unionDog.name = "Rex";
			unionDog.goodBoy = true;
			unionDog.owner = unionPerson;
			session.persist( unionDog );
			final var unionCat = new UnionCat();
			unionCat.id = 2L;
			unionCat.name = "Tom";
			unionCat.lives = 9;
			unionCat.owner = unionPerson;
			session.persist( unionCat );
			long unionToyId = 1;
			for ( var label : List.of( "ball", "bone", "mouse" ) ) {
				final var toy = new UnionToy();
				toy.id = unionToyId++;
				toy.label = label;
				toy.animal = label.equals( "mouse" ) ? unionCat : unionDog;
				session.persist( toy );
			}

			final var article = new Article();
			article.id = 1L;
			article.createdBy = "author";
			article.comments.add( "first" );
			article.comments.add( "second" );
			session.persist( article );
			for ( long id = 1; id <= 2; id++ ) {
				final var paragraph = new Paragraph();
				paragraph.id = id;
				paragraph.createdBy = "editor " + id;
				paragraph.text = "paragraph " + id;
				paragraph.article = article;
				session.persist( paragraph );
			}
		} );
		scope.getCollectingStatementInspector().clear();
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	private static void assertNestedSql(boolean expected, SessionFactoryScope scope) {
		final boolean nested = scope.getCollectingStatementInspector().getSqlQueries().stream()
				.anyMatch( sql -> sql.contains( "json" ) );
		assertThat( nested ).isEqualTo( expected );
	}

	// ~~~ the elements belong to an entity hierarchy: secondary select

	@Test
	void singleTableElements_BY_ID(SessionFactoryScope scope) {
		elements( SinglePerson.class, FetchMethod.BY_ID, scope );
	}

	@Test
	void singleTableElements_NESTED(SessionFactoryScope scope) {
		elements( SinglePerson.class, FetchMethod.NESTED, scope );
	}

	@Test
	void joinedElements_BY_ID(SessionFactoryScope scope) {
		elements( JoinedPerson.class, FetchMethod.BY_ID, scope );
	}

	@Test
	void joinedElements_NESTED(SessionFactoryScope scope) {
		elements( JoinedPerson.class, FetchMethod.NESTED, scope );
	}

	@Test
	void tablePerClassElements_BY_ID(SessionFactoryScope scope) {
		elements( UnionPerson.class, FetchMethod.BY_ID, scope );
	}

	@Test
	void tablePerClassElements_NESTED(SessionFactoryScope scope) {
		elements( UnionPerson.class, FetchMethod.NESTED, scope );
	}

	private void elements(Class<? extends Person> personType, FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( personType );
			graph.addAttributeNode( "pets" ).addOption( method );
			final Person person = session.find( graph, 1L );
			// the row of a hierarchy has no fixed shape, so the elements are not fetched nested
			assertNestedSql( false, scope );
			final var pets = person.pets().stream().sorted( Comparator.comparing( Pet::id ) ).toList();
			assertThat( pets ).hasSize( 2 );
			assertThat( pets.get( 0 ) ).isInstanceOf( Dog.class );
			assertThat( ( (Dog) pets.get( 0 ) ).goodBoy() ).isTrue();
			assertThat( pets.get( 1 ) ).isInstanceOf( Cat.class );
			assertThat( ( (Cat) pets.get( 1 ) ).lives() ).isEqualTo( 9 );
		} );
	}

	// ~~~ the owners belong to an entity hierarchy: nested

	@Test
	void singleTableOwners_BY_ID(SessionFactoryScope scope) {
		owners( "SingleAnimal", SingleAnimal.class, FetchMethod.BY_ID, scope );
	}

	@Test
	void singleTableOwners_NESTED(SessionFactoryScope scope) {
		owners( "SingleAnimal", SingleAnimal.class, FetchMethod.NESTED, scope );
	}

	@Test
	void joinedOwners_BY_ID(SessionFactoryScope scope) {
		owners( "JoinedAnimal", JoinedAnimal.class, FetchMethod.BY_ID, scope );
	}

	@Test
	void joinedOwners_NESTED(SessionFactoryScope scope) {
		owners( "JoinedAnimal", JoinedAnimal.class, FetchMethod.NESTED, scope );
	}

	@Test
	void tablePerClassOwners_BY_ID(SessionFactoryScope scope) {
		owners( "UnionAnimal", UnionAnimal.class, FetchMethod.BY_ID, scope );
	}

	@Test
	void tablePerClassOwners_NESTED(SessionFactoryScope scope) {
		owners( "UnionAnimal", UnionAnimal.class, FetchMethod.NESTED, scope );
	}

	private <A extends Pet> void owners(String entityName, Class<A> animalType, FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( animalType );
			graph.addAttributeNode( "toys" ).addOption( method );
			// a polymorphic query, returning dogs and cats
			final var animals = session.createSelectionQuery( "from " + entityName + " a order by a.id", graph )
					.getResultList();
			assertNestedSql( method == FetchMethod.NESTED, scope );
			assertThat( animals ).hasSize( 2 );
			assertThat( animals.get( 0 ) ).isInstanceOf( Dog.class );
			assertThat( animals.get( 0 ).toyLabels() ).containsExactlyInAnyOrder( "ball", "bone" );
			assertThat( animals.get( 1 ) ).isInstanceOf( Cat.class );
			assertThat( animals.get( 1 ).toyLabels() ).containsExactly( "mouse" );
		} );
	}

	// ~~~ mapped superclasses: nested

	@Test
	void elementExtendsMappedSuperclass_BY_ID(SessionFactoryScope scope) {
		elementExtendsMappedSuperclass( FetchMethod.BY_ID, scope );
	}

	@Test
	void elementExtendsMappedSuperclass_NESTED(SessionFactoryScope scope) {
		elementExtendsMappedSuperclass( FetchMethod.NESTED, scope );
	}

	private void elementExtendsMappedSuperclass(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Article.class );
			graph.addAttributeNode( "paragraphs" ).addOption( method );
			final var article = session.find( graph, 1L );
			assertNestedSql( method == FetchMethod.NESTED, scope );
			final var paragraphs = article.paragraphs.stream().sorted( Comparator.comparing( p -> p.id ) ).toList();
			assertThat( paragraphs ).extracting( p -> p.text ).containsExactly( "paragraph 1", "paragraph 2" );
			// the columns inherited from the mapped superclass
			assertThat( paragraphs ).extracting( p -> p.createdBy ).containsExactly( "editor 1", "editor 2" );
			assertThat( paragraphs ).extracting( p -> p.version ).containsExactly( 0, 0 );
		} );
	}

	@Test
	void collectionOfMappedSuperclass_BY_ID(SessionFactoryScope scope) {
		collectionOfMappedSuperclass( FetchMethod.BY_ID, scope );
	}

	@Test
	void collectionOfMappedSuperclass_NESTED(SessionFactoryScope scope) {
		collectionOfMappedSuperclass( FetchMethod.NESTED, scope );
	}

	private void collectionOfMappedSuperclass(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Article.class );
			graph.addAttributeNode( "comments" ).addOption( method );
			final var article = session.find( graph, 1L );
			assertNestedSql( method == FetchMethod.NESTED, scope );
			assertThat( article.comments ).containsExactlyInAnyOrder( "first", "second" );
		} );
	}

	// ~~~ the model

	interface Person {
		List<? extends Pet> pets();
	}

	interface Pet {
		Long id();

		List<String> toyLabels();
	}

	interface Dog {
		boolean goodBoy();
	}

	interface Cat {
		int lives();
	}

	@Entity(name = "SinglePerson")
	@Table(name = "nfi_single_person")
	static class SinglePerson implements Person {
		@Id
		Long id;
		@OneToMany(mappedBy = "owner")
		List<SingleAnimal> pets = new ArrayList<>();

		@Override
		public List<? extends Pet> pets() {
			return pets;
		}
	}

	@Entity(name = "SingleAnimal")
	@Table(name = "nfi_single_animal")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	abstract static class SingleAnimal implements Pet {
		@Id
		Long id;
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		SinglePerson owner;
		@OneToMany(mappedBy = "animal")
		List<SingleToy> toys = new ArrayList<>();

		@Override
		public Long id() {
			return id;
		}

		@Override
		public List<String> toyLabels() {
			return toys.stream().map( toy -> toy.label ).toList();
		}
	}

	@Entity(name = "SingleDog")
	static class SingleDog extends SingleAnimal implements Dog {
		boolean goodBoy;

		@Override
		public boolean goodBoy() {
			return goodBoy;
		}
	}

	@Entity(name = "SingleCat")
	static class SingleCat extends SingleAnimal implements Cat {
		int lives;

		@Override
		public int lives() {
			return lives;
		}
	}

	@Entity(name = "SingleToy")
	@Table(name = "nfi_single_toy")
	static class SingleToy {
		@Id
		Long id;
		String label;
		@ManyToOne(fetch = FetchType.LAZY)
		SingleAnimal animal;
	}

	@Entity(name = "JoinedPerson")
	@Table(name = "nfi_joined_person")
	static class JoinedPerson implements Person {
		@Id
		Long id;
		@OneToMany(mappedBy = "owner")
		List<JoinedAnimal> pets = new ArrayList<>();

		@Override
		public List<? extends Pet> pets() {
			return pets;
		}
	}

	@Entity(name = "JoinedAnimal")
	@Table(name = "nfi_joined_animal")
	@Inheritance(strategy = InheritanceType.JOINED)
	abstract static class JoinedAnimal implements Pet {
		@Id
		Long id;
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		JoinedPerson owner;
		@OneToMany(mappedBy = "animal")
		List<JoinedToy> toys = new ArrayList<>();

		@Override
		public Long id() {
			return id;
		}

		@Override
		public List<String> toyLabels() {
			return toys.stream().map( toy -> toy.label ).toList();
		}
	}

	@Entity(name = "JoinedDog")
	@Table(name = "nfi_joined_dog")
	static class JoinedDog extends JoinedAnimal implements Dog {
		boolean goodBoy;

		@Override
		public boolean goodBoy() {
			return goodBoy;
		}
	}

	@Entity(name = "JoinedCat")
	@Table(name = "nfi_joined_cat")
	static class JoinedCat extends JoinedAnimal implements Cat {
		int lives;

		@Override
		public int lives() {
			return lives;
		}
	}

	@Entity(name = "JoinedToy")
	@Table(name = "nfi_joined_toy")
	static class JoinedToy {
		@Id
		Long id;
		String label;
		@ManyToOne(fetch = FetchType.LAZY)
		JoinedAnimal animal;
	}

	@Entity(name = "UnionPerson")
	@Table(name = "nfi_union_person")
	static class UnionPerson implements Person {
		@Id
		Long id;
		@OneToMany(mappedBy = "owner")
		List<UnionAnimal> pets = new ArrayList<>();

		@Override
		public List<? extends Pet> pets() {
			return pets;
		}
	}

	@Entity(name = "UnionAnimal")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	abstract static class UnionAnimal implements Pet {
		@Id
		Long id;
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		UnionPerson owner;
		@OneToMany(mappedBy = "animal")
		List<UnionToy> toys = new ArrayList<>();

		@Override
		public Long id() {
			return id;
		}

		@Override
		public List<String> toyLabels() {
			return toys.stream().map( toy -> toy.label ).toList();
		}
	}

	@Entity(name = "UnionDog")
	@Table(name = "nfi_union_dog")
	static class UnionDog extends UnionAnimal implements Dog {
		boolean goodBoy;

		@Override
		public boolean goodBoy() {
			return goodBoy;
		}
	}

	@Entity(name = "UnionCat")
	@Table(name = "nfi_union_cat")
	static class UnionCat extends UnionAnimal implements Cat {
		int lives;

		@Override
		public int lives() {
			return lives;
		}
	}

	@Entity(name = "UnionToy")
	@Table(name = "nfi_union_toy")
	static class UnionToy {
		@Id
		Long id;
		String label;
		@ManyToOne(fetch = FetchType.LAZY)
		UnionAnimal animal;
	}

	@MappedSuperclass
	abstract static class BaseEntity {
		@Id
		Long id;
		@Version
		int version;
		String createdBy;
	}

	@MappedSuperclass
	abstract static class Commented extends BaseEntity {
		@ElementCollection
		Set<String> comments = new HashSet<>();
	}

	@Entity(name = "NfiArticle")
	@Table(name = "nfi_article")
	static class Article extends Commented {
		@OneToMany(mappedBy = "article")
		List<Paragraph> paragraphs = new ArrayList<>();
	}

	@Entity(name = "NfiParagraph")
	@Table(name = "nfi_paragraph")
	static class Paragraph extends BaseEntity {
		String text;
		@ManyToOne(fetch = FetchType.LAZY)
		Article article;
	}
}
