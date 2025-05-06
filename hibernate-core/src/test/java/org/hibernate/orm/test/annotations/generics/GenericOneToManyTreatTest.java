/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		GenericOneToManyTreatTest.Configuration.class,
		GenericOneToManyTreatTest.AConfiguration.class,
		GenericOneToManyTreatTest.AItem.class,
		GenericOneToManyTreatTest.BConfiguration.class,
		GenericOneToManyTreatTest.BItem.class,
		GenericOneToManyTreatTest.BItemChild.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16693" )
public class GenericOneToManyTreatTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final BConfiguration bConfiguration = new BConfiguration();
			final BItem bItem = new BItem( bConfiguration );
			final BItemChild bItemChild = new BItemChild( bItem, "b_item_child" );
			bItem.getChildren().add( bItemChild );
			bConfiguration.getItems().add( bItem );
			session.persist( bItemChild );
			session.persist( bItem );
			session.persist( bConfiguration );
			final AConfiguration aConfiguration = new AConfiguration();
			final AItem aItem = new AItem( aConfiguration, "basic_prop" );
			aConfiguration.getItems().add( aItem );
			session.persist( aItem );
			session.persist( aConfiguration );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from BItemChild" ).executeUpdate();
			session.createMutationQuery( "delete from AItem" ).executeUpdate();
			session.createMutationQuery( "delete from BItem" ).executeUpdate();
			session.createMutationQuery( "delete from AConfiguration" ).executeUpdate();
			session.createMutationQuery( "delete from BConfiguration" ).executeUpdate();
		} );
	}

	@Test
	public void testTreatedSetJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final BConfiguration result = session.createQuery(
					"select config from BConfiguration config "
							+ "left join treat(config.items as BItem) item "
							+ "left join item.children child ",
					BConfiguration.class
			).getSingleResult();
			assertThat( result.getItems() ).hasSize( 1 );
			final BItem bItem = result.getItems().iterator().next();
			assertThat( bItem.getChildren() ).hasSize( 1 );
			assertThat( bItem.getChildren().iterator().next().getName() ).isEqualTo( "b_item_child" );
		} );
	}

	@Test
	public void testGenericSetJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final BConfiguration result = session.createQuery(
					"select config from BConfiguration config "
							+ "left join config.items item "
							+ "left join item.children child ",
					BConfiguration.class
			).getSingleResult();
			assertThat( result.getItems() ).hasSize( 1 );
			final BItem bItem = result.getItems().iterator().next();
			assertThat( bItem.getChildren() ).hasSize( 1 );
			assertThat( bItem.getChildren().iterator().next().getName() ).isEqualTo( "b_item_child" );
		} );
	}

	@Test
	public void testTreatedBasicProp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AConfiguration result = session.createQuery(
					"select config from AConfiguration config "
							+ "left join treat(config.items as AItem) item "
							+ "where item.basicProp = 'basic_prop'",
					AConfiguration.class
			).getSingleResult();
			assertThat( result.getItems() ).hasSize( 1 );
			final AItem aItem = result.getItems().iterator().next();
			assertThat( aItem.getBasicProp() ).isEqualTo( "basic_prop" );
		} );
	}

	@Test
	public void testGenericBasicProp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AConfiguration result = session.createQuery(
					"select config from AConfiguration config "
							+ "left join config.items item "
							+ "where item.basicProp = 'basic_prop'",
					AConfiguration.class
			).getSingleResult();
			assertThat( result.getItems() ).hasSize( 1 );
			final AItem aItem = result.getItems().iterator().next();
			assertThat( aItem.getBasicProp() ).isEqualTo( "basic_prop" );
		} );
	}

	@MappedSuperclass
	public abstract static class Configuration<T> {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany( mappedBy = "configuration" )
		private Set<T> items = new HashSet<>();

		public Set<T> getItems() {
			return items;
		}
	}

	@Entity( name = "AConfiguration" )
	public static class AConfiguration extends Configuration<AItem> {
	}

	@Entity( name = "AItem" )
	public static class AItem {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		@JoinColumn( name = "configuration_id" )
		private AConfiguration configuration;

		private String basicProp;

		public AItem() {
		}

		public AItem(AConfiguration configuration, String basicProp) {
			this.configuration = configuration;
			this.basicProp = basicProp;
		}

		public String getBasicProp() {
			return basicProp;
		}
	}

	@Entity( name = "BConfiguration" )
	public static class BConfiguration extends Configuration<BItem> {
	}

	@Entity( name = "BItem" )
	public static class BItem {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		@JoinColumn( name = "configuration_id" )
		private BConfiguration configuration;

		@OneToMany( mappedBy = "item" )
		private Set<BItemChild> children = new HashSet<>();

		public BItem() {
		}

		public BItem(BConfiguration configuration) {
			this.configuration = configuration;
		}

		public Set<BItemChild> getChildren() {
			return children;
		}
	}

	@Entity( name = "BItemChild" )
	public static class BItemChild {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		@JoinColumn( name = "bitem_id" )
		private BItem item;

		private String name;

		public BItemChild() {
		}

		public BItemChild(BItem item, String name) {
			this.item = item;
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
