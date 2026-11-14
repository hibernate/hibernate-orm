package org.hibernate.orm.test.entitygraph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.LockModeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.FetchMethod;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SortNatural;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks {@link FetchMethod#NESTED} together with other features of Hibernate.
 * Every case runs with {@link FetchMethod#BY_ID}, as the reference, and with
 * {@link FetchMethod#NESTED}, which must give the same results using the
 * nested subquery.
 */
@DomainModel(
		annotatedClasses = {
				EntityGraphNestedFetchFeaturesTest.Shop.class,
				EntityGraphNestedFetchFeaturesTest.Product.class,
				EntityGraphNestedFetchFeaturesTest.Coupon.class,
				EntityGraphNestedFetchFeaturesTest.Promo.class,
				EntityGraphNestedFetchFeaturesTest.Supplier.class,
				EntityGraphNestedFetchFeaturesTest.Org.class,
				EntityGraphNestedFetchFeaturesTest.Member.class,
				EntityGraphNestedFetchFeaturesTest.Base.class,
				EntityGraphNestedFetchFeaturesTest.Sub.class,
				EntityGraphNestedFetchFeaturesTest.SubItem.class,
				EntityGraphNestedFetchFeaturesTest.Owner.class,
				EntityGraphNestedFetchFeaturesTest.Item.class,
				EntityGraphNestedFetchFeaturesTest.Target.class,
				EntityGraphNestedFetchFeaturesTest.Detail.class
		}
)
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		@Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true")
})
@SessionFactory(useCollectingStatementInspector = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNestedCollectionFetch.class)
@JiraKey("HHH-9558")
class EntityGraphNestedFetchFeaturesTest {

	private static final String SPECIAL_CHARACTERS = "café 😀 \"q\" \\ \n\t end";

	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var supplier = new Supplier( 1L, "acme" );
			session.persist( supplier );
			for ( long id = 1; id <= 2; id++ ) {
				final var shop = new Shop();
				shop.id = id;
				shop.code = "S" + id;
				shop.info = new Info();
				shop.info.emails.add( "a" + id + "@x" );
				shop.info.emails.add( "b" + id + "@x" );
				shop.labels.addAll( List.of( "zeta", "alpha", "mid" ) );
				shop.lines.add( new Line( "sku-" + id, supplier ) );
				shop.cachedTags.add( "t" + id );
				session.persist( shop );
				for ( int p = 1; p <= 2; p++ ) {
					final var product = new Product();
					product.id = id * 10 + p;
					product.name = p == 1 ? SPECIAL_CHARACTERS : "plain";
					product.price = new Money( 100 * p );
					product.shop = shop;
					product.featuredIn = p == 2 ? shop : null;
					session.persist( product );
					final var promo = new Promo();
					promo.id = id * 10 + p;
					promo.active = p == 1;
					promo.shop = shop;
					session.persist( promo );
				}
				final var coupon = new Coupon();
				coupon.id = id;
				coupon.shopCode = shop.code;
				session.persist( coupon );
			}

			final var org = new Org();
			org.id = new OrgId( "x", 1 );
			session.persist( org );
			final var sub = new Sub();
			sub.id = 1L;
			session.persist( sub );
			for ( long i = 1; i <= 2; i++ ) {
				final var member = new Member();
				member.id = i;
				member.org = org;
				session.persist( member );
				final var item = new SubItem();
				item.id = i;
				item.sub = sub;
				session.persist( item );
			}

			final var target = new Target();
			target.id = 10L;
			target.name = "t10";
			session.persist( target );
			final var owner = new Owner();
			owner.id = 1L;
			session.persist( owner );
			session.flush();
			// item 1 refers to an existing target and has a detail, item 2 refers to a missing target
			session.createNativeMutationQuery( "insert into nf_item (id, owner_id, target_id) values (1, 1, 10)" )
					.executeUpdate();
			session.createNativeMutationQuery( "insert into nf_item (id, owner_id, target_id) values (2, 1, 99)" )
					.executeUpdate();
			session.createNativeMutationQuery( "insert into nf_detail (id, item_id, text) values (5, 1, 'd1')" )
					.executeUpdate();
		} );
		scope.getSessionFactory().getCache().evictAllRegions();
		scope.getSessionFactory().getStatistics().clear();
		scope.getCollectingStatementInspector().clear();
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	private static RootGraph<Shop> graph(Session session, FetchMethod method, String... attributes) {
		final var graph = session.createEntityGraph( Shop.class );
		for ( var attribute : attributes ) {
			graph.addAttributeNode( attribute ).addOption( method );
		}
		return graph;
	}

	/**
	 * With {@code NESTED}, the collection must have been fetched by the nested subquery.
	 */
	private static void assertFetchedBy(FetchMethod method, SessionFactoryScope scope) {
		final boolean nested = scope.getCollectingStatementInspector().getSqlQueries().stream()
				.anyMatch( sql -> sql.contains( "json" ) );
		assertThat( nested ).isEqualTo( method == FetchMethod.NESTED );
	}

	@Test
	void convertedVersionedAndSpecialCharacters_BY_ID(SessionFactoryScope scope) {
		convertedVersionedAndSpecialCharacters( FetchMethod.BY_ID, scope );
	}

	@Test
	void convertedVersionedAndSpecialCharacters_NESTED(SessionFactoryScope scope) {
		convertedVersionedAndSpecialCharacters( FetchMethod.NESTED, scope );
	}

	private void convertedVersionedAndSpecialCharacters(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var shop = session.find( graph( session, method, "products" ), 1L );
			assertFetchedBy( method, scope );
			final var products = shop.products.stream().sorted( Comparator.comparing( p -> p.id ) ).toList();
			assertThat( products ).hasSize( 2 );
			assertThat( products.get( 0 ).name ).isEqualTo( SPECIAL_CHARACTERS );
			assertThat( products.get( 1 ).price.cents() ).isEqualTo( 200 );
			assertThat( products.get( 0 ).version ).isEqualTo( 0 );
		} );
	}

	@Test
	void twoCollectionsOfSameElementType_BY_ID(SessionFactoryScope scope) {
		twoCollectionsOfSameElementType( FetchMethod.BY_ID, scope );
	}

	@Test
	void twoCollectionsOfSameElementType_NESTED(SessionFactoryScope scope) {
		twoCollectionsOfSameElementType( FetchMethod.NESTED, scope );
	}

	private void twoCollectionsOfSameElementType(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var shop = session.find( graph( session, method, "products", "featured" ), 1L );
			assertFetchedBy( method, scope );
			assertThat( shop.products ).hasSize( 2 );
			assertThat( shop.featured ).hasSize( 1 );
			// the same managed instance in both collections
			assertThat( shop.products ).contains( shop.featured.get( 0 ) );
		} );
	}

	@Test
	void collectionKeyIsNotPrimaryKey_BY_ID(SessionFactoryScope scope) {
		collectionKeyIsNotPrimaryKey( FetchMethod.BY_ID, scope );
	}

	@Test
	void collectionKeyIsNotPrimaryKey_NESTED(SessionFactoryScope scope) {
		collectionKeyIsNotPrimaryKey( FetchMethod.NESTED, scope );
	}

	private void collectionKeyIsNotPrimaryKey(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var shops = session.createSelectionQuery( "from NfShop s order by s.id", graph( session, method, "coupons" ) )
					.getResultList();
			assertFetchedBy( method, scope );
			assertThat( shops.get( 0 ).coupons ).extracting( c -> c.id ).containsExactly( 1L );
			assertThat( shops.get( 1 ).coupons ).extracting( c -> c.id ).containsExactly( 2L );
		} );
	}

	@Test
	void filterOnCollection_BY_ID(SessionFactoryScope scope) {
		filterOnCollection( FetchMethod.BY_ID, scope );
	}

	@Test
	void filterOnCollection_NESTED(SessionFactoryScope scope) {
		filterOnCollection( FetchMethod.NESTED, scope );
	}

	private void filterOnCollection(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "nfActiveOnly" );
			final var shop = session.find( graph( session, method, "promos" ), 1L );
			assertFetchedBy( method, scope );
			assertThat( shop.promos ).extracting( p -> p.id ).containsExactly( 11L );
		} );
	}

	@Test
	void collectionInEmbeddable_BY_ID(SessionFactoryScope scope) {
		collectionInEmbeddable( FetchMethod.BY_ID, scope );
	}

	@Test
	void collectionInEmbeddable_NESTED(SessionFactoryScope scope) {
		collectionInEmbeddable( FetchMethod.NESTED, scope );
	}

	private void collectionInEmbeddable(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Shop.class );
			graph.addSubgraph( "info" ).addAttributeNode( "emails" ).addOption( method );
			final var shops = session.createSelectionQuery( "from NfShop s order by s.id", graph ).getResultList();
			assertFetchedBy( method, scope );
			assertThat( Hibernate.isInitialized( shops.get( 0 ).info.emails ) ).isTrue();
			assertThat( shops.get( 0 ).info.emails ).containsExactlyInAnyOrder( "a1@x", "b1@x" );
			assertThat( shops.get( 1 ).info.emails ).containsExactlyInAnyOrder( "a2@x", "b2@x" );
		} );
	}

	@Test
	void sortedSetAndEmbeddableWithToOne_BY_ID(SessionFactoryScope scope) {
		sortedSetAndEmbeddableWithToOne( FetchMethod.BY_ID, scope );
	}

	@Test
	void sortedSetAndEmbeddableWithToOne_NESTED(SessionFactoryScope scope) {
		sortedSetAndEmbeddableWithToOne( FetchMethod.NESTED, scope );
	}

	private void sortedSetAndEmbeddableWithToOne(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var shop = session.find( graph( session, method, "labels", "lines" ), 1L );
			assertFetchedBy( method, scope );
			assertThat( shop.labels ).containsExactly( "alpha", "mid", "zeta" );
			assertThat( shop.lines ).hasSize( 1 );
			assertThat( shop.lines.get( 0 ).sku ).isEqualTo( "sku-1" );
			assertThat( Hibernate.unproxy( shop.lines.get( 0 ).supplier, Supplier.class ).name ).isEqualTo( "acme" );
		} );
	}

	@Test
	void collectionSecondLevelCache_BY_ID(SessionFactoryScope scope) {
		collectionSecondLevelCache( FetchMethod.BY_ID, scope );
	}

	@Test
	void collectionSecondLevelCache_NESTED(SessionFactoryScope scope) {
		collectionSecondLevelCache( FetchMethod.NESTED, scope );
	}

	private void collectionSecondLevelCache(FetchMethod method, SessionFactoryScope scope) {
		final var statistics = scope.getSessionFactory().getStatistics()
				.getCollectionStatistics( Shop.class.getName() + ".cachedTags" );
		scope.inTransaction( session -> {
			final var shop = session.find( graph( session, method, "cachedTags" ), 1L );
			assertFetchedBy( method, scope );
			assertThat( shop.cachedTags ).containsExactly( "t1" );
		} );
		assertThat( statistics.getCachePutCount() ).isEqualTo( 1 );
		scope.inTransaction( session -> assertThat( session.find( Shop.class, 1L ).cachedTags ).containsExactly( "t1" ) );
		assertThat( statistics.getCacheHitCount() ).isEqualTo( 1 );
	}

	@Test
	void distinctQuery_BY_ID(SessionFactoryScope scope) {
		distinctQuery( FetchMethod.BY_ID, scope );
	}

	@Test
	void distinctQuery_NESTED(SessionFactoryScope scope) {
		distinctQuery( FetchMethod.NESTED, scope );
	}

	private void distinctQuery(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var shops = session.createSelectionQuery( "select distinct s from NfShop s order by s.id",
							graph( session, method, "products" ) )
					.getResultList();
			assertFetchedBy( method, scope );
			assertThat( shops ).hasSize( 2 );
			assertThat( shops.get( 0 ).products ).hasSize( 2 );
		} );
	}

	@Test
	void pessimisticLock_BY_ID(SessionFactoryScope scope) {
		pessimisticLock( FetchMethod.BY_ID, scope );
	}

	@Test
	void pessimisticLock_NESTED(SessionFactoryScope scope) {
		pessimisticLock( FetchMethod.NESTED, scope );
	}

	private void pessimisticLock(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var shop = session.find( graph( session, method, "products" ), 1L, LockModeType.PESSIMISTIC_WRITE );
			assertFetchedBy( method, scope );
			assertThat( shop.products ).hasSize( 2 );
		} );
	}

	@Test
	void resultCount_BY_ID(SessionFactoryScope scope) {
		resultCount( FetchMethod.BY_ID, scope );
	}

	@Test
	void resultCount_NESTED(SessionFactoryScope scope) {
		resultCount( FetchMethod.NESTED, scope );
	}

	private void resultCount(FetchMethod method, SessionFactoryScope scope) {
		// the count query fetches nothing
		scope.inTransaction( session -> assertThat(
				session.createSelectionQuery( "from NfShop s", graph( session, method, "products" ) ).getResultCount() )
				.isEqualTo( 2 ) );
	}

	@Test
	void resultStream_BY_ID(SessionFactoryScope scope) {
		resultStream( FetchMethod.BY_ID, scope );
	}

	@Test
	void resultStream_NESTED(SessionFactoryScope scope) {
		resultStream( FetchMethod.NESTED, scope );
	}

	private void resultStream(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try ( var stream = session.createSelectionQuery( "from NfShop s order by s.id",
					graph( session, method, "products", "labels" ) ).getResultStream() ) {
				final var shops = stream.toList();
				assertFetchedBy( method, scope );
				assertThat( shops ).hasSize( 2 );
				assertThat( shops.get( 0 ).products ).hasSize( 2 );
				assertThat( shops.get( 1 ).labels ).hasSize( 3 );
			}
		} );
	}

	@Test
	void findMultiple_BY_ID(SessionFactoryScope scope) {
		findMultiple( FetchMethod.BY_ID, scope );
	}

	@Test
	void findMultiple_NESTED(SessionFactoryScope scope) {
		findMultiple( FetchMethod.NESTED, scope );
	}

	private void findMultiple(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var shops = session.findMultiple( graph( session, method, "products" ), List.of( 1L, 2L ) );
			assertFetchedBy( method, scope );
			assertThat( shops ).hasSize( 2 );
			assertThat( shops ).allSatisfy( shop -> assertThat( shop.products ).hasSize( 2 ) );
		} );
	}

	@Test
	void statelessSession_BY_ID(SessionFactoryScope scope) {
		statelessSession( FetchMethod.BY_ID, scope );
	}

	@Test
	void statelessSession_NESTED(SessionFactoryScope scope) {
		statelessSession( FetchMethod.NESTED, scope );
	}

	private void statelessSession(FetchMethod method, SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			final var graph = session.createEntityGraph( Shop.class );
			graph.addAttributeNode( "products" ).addOption( method );
			final var shop = session.get( graph, GraphSemantic.FETCH, 1L );
			assertFetchedBy( method, scope );
			assertThat( Hibernate.isInitialized( shop.products ) ).isTrue();
			assertThat( shop.products ).hasSize( 2 );
		} );
	}

	@Test
	void compositeIdOwner_BY_ID(SessionFactoryScope scope) {
		compositeIdOwner( FetchMethod.BY_ID, scope );
	}

	@Test
	void compositeIdOwner_NESTED(SessionFactoryScope scope) {
		compositeIdOwner( FetchMethod.NESTED, scope );
	}

	private void compositeIdOwner(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Org.class );
			graph.addAttributeNode( "members" ).addOption( method );
			final var org = session.find( graph, new OrgId( "x", 1 ) );
			assertFetchedBy( method, scope );
			assertThat( org.members ).extracting( m -> m.id ).containsExactlyInAnyOrder( 1L, 2L );
		} );
	}

	@Test
	void joinedSubclassOwner_BY_ID(SessionFactoryScope scope) {
		joinedSubclassOwner( FetchMethod.BY_ID, scope );
	}

	@Test
	void joinedSubclassOwner_NESTED(SessionFactoryScope scope) {
		joinedSubclassOwner( FetchMethod.NESTED, scope );
	}

	private void joinedSubclassOwner(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Sub.class );
			graph.addAttributeNode( "items" ).addOption( method );
			final var subs = session.createSelectionQuery( "from NfSub", graph ).getResultList();
			assertFetchedBy( method, scope );
			assertThat( subs.get( 0 ).items ).hasSize( 2 );
		} );
	}

	@Test
	void notFoundAndInverseOneToOneInElement_BY_ID(SessionFactoryScope scope) {
		notFoundAndInverseOneToOneInElement( FetchMethod.BY_ID, scope );
	}

	@Test
	void notFoundAndInverseOneToOneInElement_NESTED(SessionFactoryScope scope) {
		notFoundAndInverseOneToOneInElement( FetchMethod.NESTED, scope );
	}

	private void notFoundAndInverseOneToOneInElement(FetchMethod method, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Owner.class );
			graph.addAttributeNode( "items" ).addOption( method );
			final var owner = session.find( graph, 1L );
			assertFetchedBy( method, scope );
			final var items = owner.items.stream().sorted( Comparator.comparing( i -> i.id ) ).toList();
			assertThat( items ).hasSize( 2 );
			assertThat( items.get( 0 ).target.name ).isEqualTo( "t10" );
			assertThat( items.get( 1 ).target ).isNull();
			assertThat( items.get( 0 ).detail.text ).isEqualTo( "d1" );
			assertThat( items.get( 1 ).detail ).isNull();
		} );
	}

	@FilterDef(name = "nfActiveOnly")
	@Entity(name = "NfShop")
	@Table(name = "nf_shop")
	static class Shop {
		@Id
		Long id;
		@Column(unique = true)
		String code;
		@Embedded
		Info info;
		@OneToMany(mappedBy = "shop")
		List<Product> products = new ArrayList<>();
		@OneToMany(mappedBy = "featuredIn")
		List<Product> featured = new ArrayList<>();
		@OneToMany
		@JoinColumn(name = "shopCode", referencedColumnName = "code", insertable = false, updatable = false)
		List<Coupon> coupons = new ArrayList<>();
		@OneToMany(mappedBy = "shop")
		@Filter(name = "nfActiveOnly", condition = "active = true")
		List<Promo> promos = new ArrayList<>();
		@ElementCollection
		@SortNatural
		SortedSet<String> labels = new TreeSet<>();
		@ElementCollection
		List<Line> lines = new ArrayList<>();
		@ElementCollection
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		Set<String> cachedTags = new HashSet<>();
	}

	@Embeddable
	static class Info {
		@ElementCollection
		Set<String> emails = new HashSet<>();
	}

	@Embeddable
	static class Line {
		String sku;
		@ManyToOne(fetch = FetchType.LAZY)
		Supplier supplier;

		Line() {
		}

		Line(String sku, Supplier supplier) {
			this.sku = sku;
			this.supplier = supplier;
		}
	}

	record Money(int cents) {
	}

	static class MoneyConverter implements AttributeConverter<Money, Integer> {
		@Override
		public Integer convertToDatabaseColumn(Money money) {
			return money == null ? null : money.cents();
		}

		@Override
		public Money convertToEntityAttribute(Integer cents) {
			return cents == null ? null : new Money( cents );
		}
	}

	@Entity(name = "NfProduct")
	@Table(name = "nf_product")
	static class Product {
		@Id
		Long id;
		@Version
		int version;
		String name;
		@Convert(converter = MoneyConverter.class)
		Money price;
		@ManyToOne(fetch = FetchType.LAZY)
		Shop shop;
		@ManyToOne(fetch = FetchType.LAZY)
		Shop featuredIn;
	}

	@Entity(name = "NfCoupon")
	@Table(name = "nf_coupon")
	static class Coupon {
		@Id
		Long id;
		String shopCode;
	}

	@Entity(name = "NfPromo")
	@Table(name = "nf_promo")
	static class Promo {
		@Id
		Long id;
		boolean active;
		@ManyToOne(fetch = FetchType.LAZY)
		Shop shop;
	}

	@Entity(name = "NfSupplier")
	@Table(name = "nf_supplier")
	static class Supplier {
		@Id
		Long id;
		String name;

		Supplier() {
		}

		Supplier(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Embeddable
	record OrgId(String region, int number) implements Serializable {
	}

	@Entity(name = "NfOrg")
	@Table(name = "nf_org")
	static class Org {
		@EmbeddedId
		OrgId id;
		@OneToMany(mappedBy = "org")
		List<Member> members = new ArrayList<>();
	}

	@Entity(name = "NfMember")
	@Table(name = "nf_member")
	static class Member {
		@Id
		Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumns({
				@JoinColumn(name = "org_region", referencedColumnName = "region"),
				@JoinColumn(name = "org_number", referencedColumnName = "number")
		})
		Org org;
	}

	@Entity(name = "NfBase")
	@Table(name = "nf_base")
	@Inheritance(strategy = InheritanceType.JOINED)
	static class Base {
		@Id
		Long id;
	}

	@Entity(name = "NfSub")
	@Table(name = "nf_sub")
	static class Sub extends Base {
		@OneToMany(mappedBy = "sub")
		List<SubItem> items = new ArrayList<>();
	}

	@Entity(name = "NfSubItem")
	@Table(name = "nf_sub_item")
	static class SubItem {
		@Id
		Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		Sub sub;
	}

	@Entity(name = "NfOwner")
	@Table(name = "nf_owner")
	static class Owner {
		@Id
		Long id;
		@OneToMany(mappedBy = "owner")
		List<Item> items = new ArrayList<>();
	}

	@Entity(name = "NfItem")
	@Table(name = "nf_item")
	static class Item {
		@Id
		Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		Owner owner;
		@ManyToOne
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
		Target target;
		@OneToOne(mappedBy = "item")
		Detail detail;
	}

	@Entity(name = "NfTarget")
	@Table(name = "nf_target")
	static class Target {
		@Id
		Long id;
		String name;
	}

	@Entity(name = "NfDetail")
	@Table(name = "nf_detail")
	static class Detail {
		@Id
		Long id;
		@OneToOne
		Item item;
		String text;
	}
}
