/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.refresh;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Jpa(
		annotatedClasses = {
				RefreshAndCollections.Category.class,
				RefreshAndCollections.CategoryProduct.class,
				RefreshAndCollections.Product.class,
				RefreshAndCollections.Sku.class,
		}
)
@JiraKey( "HHH-16665" )
public class RefreshAndCollections {

	@Test
	public void testRefresh(EntityManagerFactoryScope scope) throws Exception {
		scope.inEntityManager(
				entityManager -> {
					EntityTransaction transaction = entityManager.getTransaction();
					try {
						transaction.begin();

						Category category = new Category( 1l, "cat 1" );
						entityManager.persist( category );

						Product product1 = new Product( 1l, "product 1" );
						new Sku(1l,"sku 1" , product1 );
						new CategoryProduct( 1l, category, product1, 1);

						Product product2 = new Product( 2l, "product 2" );
						new Sku(2l,"sku 2" , product2 );
						new CategoryProduct( 2l, category, product2, 1);

						Product product3 = new Product( 3l, "product 3" );
						new Sku(3l,"sku 3" , product3 );
						new CategoryProduct( 3l, category, product3, 1);

						entityManager.persist( product1 );
						entityManager.persist( product2 );
						entityManager.persist( product3 );

						transaction.commit();
						entityManager.clear();
						transaction.begin();

						Product product = entityManager.find( Product.class, 1l );
						product.getCategoryProducts().get( 0 );

						Category category1 = entityManager.find( Category.class, 1l );

						Product product4 = new Product( 10l, "product 10" );
						new Sku(10l,"sku 10" , product4 );
						new CategoryProduct( 10l, category1, product4, 1);

						entityManager.persist( product4 );
						entityManager.flush();

						entityManager.refresh( product );

						entityManager.refresh( product4 );

						transaction.commit();
					}
					finally {
						if ( transaction.isActive() ) {
							transaction.rollback();
						}
					}
				}
		);
	}

	@Entity(name = "Category")
	@Table(name = "CATEGORY_TABLE")
	public static class Category {

		@Id
		protected Long id;

		@Column(name = "NAME", nullable = false)
		protected String name;

		@OneToMany(mappedBy = "category", cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
		@OrderBy(value = "displayOrder")
		protected List<CategoryProduct> products = new ArrayList<>();

		public Category() {
		}

		public Category(Long id, String name) {
			this.id = id;
			this.name = name;
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

		public List<CategoryProduct> getProducts() {
			return products;
		}

		public void setProducts(List<CategoryProduct> allProductXrefs) {
			this.products = allProductXrefs;
		}
	}

	@Entity(name = "CategoryProduct")
	public static class CategoryProduct {

		@Id
		protected Long id;

		@ManyToOne(optional = false, cascade = CascadeType.REFRESH)
		@JoinColumn(name = "CATEGORY_ID")
		protected Category category = new Category();

		@ManyToOne(optional = false, cascade = CascadeType.REFRESH)
		@JoinColumn(name = "PRODUCT_ID")
		protected Product product = new Product();

		protected Integer displayOrder;

		public CategoryProduct() {
		}

		public CategoryProduct(Long id, Category category, Product product, Integer displayOrder) {
			this.id = id;
			this.category = category;
			this.product = product;
			this.displayOrder = displayOrder;
			product.getCategoryProducts().add( this );
			category.getProducts().add( this );
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Category getCategory() {
			return category;
		}

		public void setCategory(Category category) {
			this.category = category;
		}

		public Product getProduct() {
			return product;
		}

		public void setProduct(Product product) {
			this.product = product;
		}

		public Integer getDisplayOrder() {
			return displayOrder;
		}

		public void setDisplayOrder(Integer displayOrder) {
			this.displayOrder = displayOrder;
		}
	}

	@Entity(name = "Product")
	public static class Product {

		@Id
		private Long id;

		private String name;

		@OneToOne(cascade = { CascadeType.ALL })
		@JoinColumn(name = "DEFAULT_SKU_ID")
		protected Sku defaultSku;

		@OneToMany(mappedBy = "product", cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
		@OrderBy(value = "displayOrder")
		protected List<CategoryProduct> categoryProducts = new ArrayList<>();

		public Product() {
		}

		public Product(Long id, String name) {
			this.id = id;
			this.name = name;
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

		public Sku getDefaultSku() {
			return defaultSku;
		}

		public void setDefaultSku(Sku defaultSku) {
			this.defaultSku = defaultSku;
		}

		public List<CategoryProduct> getCategoryProducts() {
			return categoryProducts;
		}

		public void setCategoryProducts(List<CategoryProduct> allParentCategoryXrefs) {
			this.categoryProducts = allParentCategoryXrefs;
		}
	}

	@Entity(name = "Sku")
	public static class Sku {

		@Id
		@Column(name = "SKU_ID")
		private Long id;

		private String name;

		@OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
		@JoinColumn(name = "DEFAULT_PRODUCT_ID")
		protected Product defaultProduct;

		public Sku() {
		}

		public Sku(Long id, String name, Product defaultProduct) {
			this.id = id;
			this.name = name;
			this.defaultProduct = defaultProduct;
			defaultProduct.setDefaultSku( this );
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

		public Product getDefaultProduct() {
			return defaultProduct;
		}

		public void setDefaultProduct(Product defaultProduct) {
			this.defaultProduct = defaultProduct;
		}
	}

}
