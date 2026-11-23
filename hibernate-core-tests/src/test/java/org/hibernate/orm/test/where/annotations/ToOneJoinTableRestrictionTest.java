package org.hibernate.orm.test.where.annotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.FetchType.LAZY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DomainModel(annotatedClasses = {
		ToOneJoinTableRestrictionTest.Product.class,
		ToOneJoinTableRestrictionTest.Category.class,
		ToOneJoinTableRestrictionTest.CategoryLink.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-4335")
class ToOneJoinTableRestrictionTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int id = 1; id <= 2; id++ ) {
				var category = new Category();
				category.id = id;
				session.persist( category );
				var product = new Product();
				product.id = id;
				product.featuredCategory = category;
				session.persist( product );
			}
			session.flush();
			for ( int id = 1; id <= 2; id++ ) {
				var link = new CategoryLink();
				link.productId = id;
				link.categoryId = id;
				link.main = id == 1 ? 1 : 0;
				session.persist( link );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void find(SessionFactoryScope scope) {
		scope.inSession( session -> {
			var visible = session.find( Product.class, 1 );
			assertEquals( 1, visible.mainCategory.getId() );
			assertEquals( 1, visible.featuredCategory.getId() );
			var hidden = session.find( Product.class, 2 );
			assertNull( hidden.mainCategory );
			assertNull( hidden.featuredCategory );
		} );
	}

	@Test
	void inverseOneToOne(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertEquals( 1, session.find( Category.class, 1 ).featuredProduct.getId() );
			assertNull( session.find( Category.class, 2 ).featuredProduct );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNestedJoinGroups.class)
	void joinFetch(SessionFactoryScope scope) {
		scope.inSession( session -> {
			var products = session.createSelectionQuery(
					"from Product p left join fetch p.mainCategory order by p.id", Product.class ).getResultList();
			assertEquals( 2, products.size() );
			assertEquals( 1, products.get( 0 ).mainCategory.getId() );
			assertNull( products.get( 1 ).mainCategory );
		} );
	}

	@Test
	void innerJoin(SessionFactoryScope scope) {
		scope.inSession( session -> assertEquals( 1,
				session.createSelectionQuery( "select p.id from Product p join p.mainCategory", Integer.class )
						.getSingleResult() ) );
	}

	@Test
	void repeatedInnerJoinOneToOne(SessionFactoryScope scope) {
		scope.inSession( session -> assertEquals( 1,
				session.createSelectionQuery(
						"select p.id from Product p join p.featuredCategory c1 join p.featuredCategory c2", Integer.class )
						.getSingleResult() ) );
	}

	@Entity(name = "Product")
	static class Product {
		@Id
		int id;

		@ManyToOne(fetch = LAZY)
		@JoinTable(name = "category_product", joinColumns = @JoinColumn(name = "product_id"),
				inverseJoinColumns = @JoinColumn(name = "category_id"))
		@SQLJoinTableRestriction("catmain = 1")
		Category mainCategory;

		@OneToOne(fetch = LAZY)
		@JoinTable(inverseJoinColumns = @JoinColumn(name = "featured_id"))
		@SQLJoinTableRestriction("featured_id <> 2")
		Category featuredCategory;

		public int getId() {
			return id;
		}
	}

	@Entity(name = "Category")
	static class Category {
		@Id
		int id;

		@OneToOne(mappedBy = "featuredCategory", fetch = LAZY)
		Product featuredProduct;

		public int getId() {
			return id;
		}
	}

	@Entity(name = "CategoryLink")
	@Table(name = "category_product")
	static class CategoryLink {
		@Id
		@Column(name = "product_id")
		int productId;
		@Column(name = "category_id")
		int categoryId;
		@Column(name = "catmain")
		int main;
	}
}
