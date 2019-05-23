/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.components;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-9108")
@Disabled("NPE inside StatefulPersistenceContext#isFoundInParent when resolving back reference")
public class EmbeddableWithCollectionTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Long headerId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Header.class, Item.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					Header h1 = new Header( "h1" );
					h1.addItem( new Item( "h1-item0", h1 ) );
					h1.addItem( new Item( "h1-item1", h1 ) );
					entityManager.persist( h1 );
					headerId = h1.getId();
				},

				// Revision 2
				entityManager -> {
					final Header header = entityManager.find( Header.class, headerId );
					header.addItem( new Item( "h1-item2", header ) );
					entityManager.merge( header );
				},

				// Revision 3
				entityManager -> {

					final Header header = entityManager.find( Header.class, headerId );
					header.removeItem( header.getEmbeddableWithCollection().getItems().get( 0 ) );
					entityManager.merge( header );
				},

				// Revision 4
				entityManager -> {
					final Header header = entityManager.find( Header.class, headerId );
					header.setEmbeddableWithCollection( null );
					entityManager.merge( header );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( Header.class, headerId ), contains( 1, 2, 3, 4 ) );
	}

	@DynamicTest
	public void testRevisionHistory() {
		final Header rev1 = getAuditReader().find( Header.class, headerId, 1 );
		assertThat( getHeaderItemNames( rev1 ), contains( "h1-item0", "h1-item1" ) );

		final Header rev2 = getAuditReader().find( Header.class, headerId, 2 );
		assertThat( getHeaderItemNames( rev2 ), contains( "h1-item0", "h1-item1", "h1-item2" ) );

		final Header rev3 = getAuditReader().find( Header.class, headerId, 3 );
		assertThat( getHeaderItemNames( rev3 ), contains( "h1-item1", "h1-item2" ) );

		final Header rev4 = getAuditReader().find( Header.class, headerId, 4 );
		assertThat( getHeaderItemNames( rev4 ), CollectionMatchers.isEmpty() );
	}

	private static List<String> getHeaderItemNames(Header header) {
		return header.getEmbeddableWithCollection().getItems().stream()
				.map( Item::getName )
				.collect( Collectors.toList() );
	}

	@Entity(name = "Header")
	@Table(name = "ENVERS_HEADER")
	@Audited
	public static class Header {
		private Long id;
		private String name;
		private EmbeddableWithCollection embeddableWithCollection;

		Header() {

		}

		Header(String name) {
			this.name = name;
		}

		Header(Long id, String name) {
			this( name );
			this.id = id;
		}

		@Id
		@GeneratedValue
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

		@Embedded
		public EmbeddableWithCollection getEmbeddableWithCollection() {
			return embeddableWithCollection;
		}

		public void setEmbeddableWithCollection(EmbeddableWithCollection embeddableWithCollection) {
			if ( embeddableWithCollection != null ) {
				this.embeddableWithCollection = embeddableWithCollection;
			}
			else if ( this.embeddableWithCollection != null ) {
				this.embeddableWithCollection.getItems().clear();
			}
		}

		public void addItem(Item item) {
			if ( this.embeddableWithCollection == null ) {
				this.embeddableWithCollection = new EmbeddableWithCollection();
			}
			this.embeddableWithCollection.addItem( item );
		}

		public void removeItem(Item item) {
			if ( this.embeddableWithCollection != null ) {
				this.embeddableWithCollection.removeItem( item );
			}
		}
	}

	@Entity(name = "Item")
	@Table(name = "ENVERS_ITEM")
	@Audited
	public static class Item {
		private Long id;
		private String name;
		private Header header;
		private Integer position;

		Item() {

		}

		Item(String name, Header header) {
			this.name = name;
			this.header = header;
		}

		Item(Long id, String name, Header header) {
			this( name, header );
			this.id = id;
		}

		@Id
		@GeneratedValue
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

		@ManyToOne
		@JoinColumn(name = "C_HDR", insertable = false, updatable = false, nullable = false)
		public Header getHeader() {
			return header;
		}

		public void setHeader(Header header) {
			this.header = header;
		}

		@Column(name = "C_POSITION", insertable = false, updatable = false)
		public Integer getPosition() {
			return position;
		}

		public void setPosition(Integer position) {
			this.position = position;
		}
	}

	@Embeddable
	@Audited
	public static class EmbeddableWithCollection {
		private List<Item> items;

		@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval=true)
		@JoinColumn(name = "C_HDR", foreignKey = @ForeignKey(name = "ENVERS_ITEM_FK_ENVERS_HEADER"), nullable = false)
		@OrderColumn(name = "C_POSITION", nullable = false)
		@AuditMappedBy(mappedBy = "header", positionMappedBy = "position")
		public List<Item> getItems() {
			return items;
		}

		public void setItems(List<Item> items) {
			this.items = items;
		}

		public void addItem(Item item) {
			if ( this.items == null ) {
				this.items = new ArrayList<Item>();
			}
			this.items.add( item );
		}

		public void removeItem(Item item) {
			if ( this.items != null ) {
				this.items.remove( item );
			}
		}
	}
}
