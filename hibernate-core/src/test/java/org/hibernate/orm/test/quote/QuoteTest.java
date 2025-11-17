/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.quote;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Emmanuel Bernard
 * @author Brett Meyer
 */
@DomainModel(
		annotatedClasses = {
				User.class,
				Role.class,
				Phone.class,
				House.class,
				QuoteTest.Container.class,
				QuoteTest.SimpleItem.class
		}
)
@SessionFactory
public class QuoteTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testQuoteManytoMany(SessionFactoryScope scope) {
		String role = User.class.getName() + ".roles";

		assertThat(
				scope.getMetadataImplementor().getCollectionBinding( role ).getCollectionTable().getName() ).isEqualTo(
				"User_Role" );

		scope.inTransaction(
				session -> {
					User u = new User();
					session.persist( u );
					Role r = new Role();
					session.persist( r );
					u.getRoles().add( r );
					session.flush();
					session.clear();
					u = session.get( User.class, u.getId() );
					assertThat( u.getRoles() ).hasSize( 1 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-8464")
	public void testDoubleQuoteJoinColumn(SessionFactoryScope scope) {
		User u = new User();
		scope.inTransaction(
				session -> {

					House house = new House();
					u.setHouse( house );
					session.persist( house );
					session.persist( u );
				}
		);

		scope.inTransaction(
				session -> {
					User user = session.get( User.class, u.getId() );
					assertThat( user ).isNotNull();
					assertThat( user.getHouse() ).isNotNull();
					// seems trivial, but if quoting normalization worked on the join column, these should all be the same
					assertThat( user.getHouse1() ).isEqualTo( user.getHouse().getId() );
					assertThat( user.getHouse2() ).isEqualTo( user.getHouse().getId() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-2988")
	public void testUnionSubclassEntityQuoting(SessionFactoryScope scope) {
		Container container1 = new Container();
		scope.inTransaction(
				session -> {
					Container container2 = new Container();
					SimpleItem simpleItem = new SimpleItem();

					container1.items.add( container2 );
					container1.items.add( simpleItem );
					container2.parent = container1;
					simpleItem.parent = container1;

					session.persist( simpleItem );
					session.persist( container2 );
					session.persist( container1 );
				}
		);

		scope.inTransaction(
				session -> {
					Container result = session.get( Container.class, container1.id );
					assertThat( result ).isNotNull();
					assertThat( result.items ).isNotNull();
					assertThat( result.items ).hasSize( 2 );
				}
		);

		scope.inTransaction(
				session -> {
					Container result = session.get( Container.class, container1.id );
					for ( Item item : result.items ) {
						item.parent = null;
					}
					result.items.clear();
					session.flush();
					session.createMutationQuery( "delete Item" ).executeUpdate();
				}
		);
	}

	@Entity(name = "Item")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	static abstract class Item {

		@Id
		@GeneratedValue
		@Column(name = "`ID`")
		protected long id;

		@Column(name = "_id")
		protected long _id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "`ParentID`")
		protected Container parent;
	}

	@Entity
	@Table(name = "`CoNTaiNeR`")
	static class Container extends Item {

		@OneToMany(mappedBy = "parent", targetEntity = Item.class)
		private Set<Item> items = new HashSet<>( 0 );
	}

	@Entity
	@Table(name = "`SimpleItem`")
	static class SimpleItem extends Item {
	}
}
