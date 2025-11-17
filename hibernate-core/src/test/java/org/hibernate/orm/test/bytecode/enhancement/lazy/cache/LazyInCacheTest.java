/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.cache;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.usertype.UserTypeLegacyBridge;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Luis Barreiro
 */
@DomainModel(
		annotatedClasses = {
			LazyInCacheTest.Order.class, LazyInCacheTest.Product.class, LazyInCacheTest.Tag.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
public class LazyInCacheTest {

	private Long orderId;


	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		Order order = new Order();
		Product product = new Product();
		order.products.add( product );
		order.data = "some data".getBytes( Charset.defaultCharset() );

		scope.inTransaction( em -> {
			em.persist( product );
			em.persist( order );
		} );

		orderId = order.id;
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			Order order = em.find( Order.class, orderId );
			assertEquals( 1, order.products.size() );
		} );
	}

	// --- //

	@Entity(name = "Order")
	@Table( name = "ORDER_TABLE" )
	@Cache( usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE )
	static class Order {

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;

		@OneToMany
		List<Product> products = new ArrayList<>();

		@OneToMany
		List<Tag> tags = new ArrayList<>();

		@Basic( fetch = FetchType.LAZY )
		@Type( BinaryCustomType.class )
//        @JdbcTypeCode(Types.LONGVARBINARY)
		byte[] data;
	}

	@Entity(name = "Product")
	@Table( name = "PRODUCT" )
	static class Product {

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;

		String name;
	}

	@Entity(name = "Tag")
	@Table( name = "TAG" )
	static class Tag {

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;

		String name;
	}

	public static class BinaryCustomType extends UserTypeLegacyBridge {
		public BinaryCustomType() {
			super( "binary" );
		}
	}
}
