/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.collections;

import jakarta.persistence.*;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;
import java.util.Set;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Anton Barkan
 */
public class ElementCollectionWithEntityAndFKForElementTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Element.class,
				ElementHolder.class,
				CollectionHolder.class,
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			CollectionHolder collectionHolder = new CollectionHolder( 1L, Set.of( "collectionElement" ) );
			entityManager.persist( collectionHolder );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			Element element = entityManager.find( Element.class, new ElementEmbeddedId( 1L, "collectionElement" ) );
			ElementHolder elementHolder = new ElementHolder( 2L, element );
			entityManager.persist( elementHolder );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			ElementHolder elementHolder = entityManager.find( ElementHolder.class, 2L );
			Assert.assertEquals( "collectionElement", elementHolder.getElement().getId().getElement() );
		} );
	}

	//tag::element-collection-with-entity-and-fk-to-element-example[]
	@Embeddable
	public static class ElementEmbeddedId implements Serializable {
		@Column(name = "element")
		private String element;

		@Column(name = "parent_id")
		private Long parentId;

		//Getters and setters are omitted for brevity

	//end::element-collection-with-entity-and-fk-to-element-example[]

		public ElementEmbeddedId() {
		}

		public ElementEmbeddedId(Long parentId, String element) {
			this.parentId = parentId;
			this.element = element;
		}

		public String getElement() {
			return element;
		}

		public Long getParentId() {
			return parentId;
		}

	//tag::element-collection-with-entity-and-fk-to-element-example[]
	}

	//end::collections-map-value-type-entity-key-example[]
	@Entity
	@Table(name = "elements")
	public static class Element {

		@EmbeddedId
		private ElementEmbeddedId id;

		//Getters and setters are omitted for brevity

	//end::element-collection-with-entity-and-fk-to-element-example[]

		public Element(ElementEmbeddedId id) {
			this.id = id;
		}

		public Element() {
		}

		public ElementEmbeddedId getId() {
			return id;
		}
	//tag::element-collection-with-entity-and-fk-to-element-example[]
	}

	@Entity
	@Table(name = "element_holder")
	public static class ElementHolder {

		@Id
		private Long id;

		@ManyToOne
		@JoinColumns(
				value = {
						@JoinColumn(name = "parent_id", referencedColumnName = "parent_id"),
						@JoinColumn(name = "element", referencedColumnName = "element"),
				}
		)
		private Element element;

		//Getters and setters are omitted for brevity

	//end::element-collection-with-entity-and-fk-to-element-example[]

		public ElementHolder(Long id, Element element) {
			this.id = id;
			this.element = element;
		}

		public ElementHolder() {
		}

		public Long getId() {
			return id;
		}

		public Element getElement() {
			return element;
		}
	//tag::element-collection-with-entity-and-fk-to-element-example[]
	}

	@Entity
	@Table(name = "collection_holder")
	public static class CollectionHolder {
		@Id
		private Long id;

		@ElementCollection(fetch = FetchType.EAGER)
		@Column(name = "element", nullable = false)
		@CollectionTable(name = "elements", joinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id"))
		private Set<String> elements;

		//Getters and setters are omitted for brevity

	//end::element-collection-with-entity-and-fk-to-element-example[]

		public CollectionHolder(Long id, Set<String> elements) {
			this.id = id;
			this.elements = elements;
		}

		public CollectionHolder() {
		}

		public Long getId() {
			return id;
		}

		public Set<String> getElements() {
			return elements;
		}
	//tag::element-collection-with-entity-and-fk-to-element-example[]
	}
	//end::element-collection-with-entity-and-fk-to-element-example[]
}
