/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;

import java.io.Serializable;
import java.util.Set;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = {
				ElementCollectionWithEntityAndFKForElementTest.CollectionHolder.class,
				ElementCollectionWithEntityAndFKForElementTest.ElementHolder.class,
				ElementCollectionWithEntityAndFKForElementTest.Element.class,
		}

)
@JiraKey(value = "HHH-15759")
public class ElementCollectionWithEntityAndFKForElementTest {


	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CollectionHolder collectionHolder = new CollectionHolder( 1L, Set.of( "collectionElement" ) );
					entityManager.persist( collectionHolder );

					Element element = new Element( new ElementEmbeddedId( "collectionElement2", 1L ) );
					ElementHolder elementHolder = new ElementHolder( 2L, element );
					entityManager.persist( elementHolder );
					entityManager.persist( element );
				} );

		scope.inTransaction(
				entityManager -> {
					ElementHolder elementHolder = entityManager.find( ElementHolder.class, 2L );
					assertEquals( "collectionElement2", elementHolder.getElement().getId().getElement() );
				} );
	}

	@Embeddable
	public static class ElementEmbeddedId implements Serializable {
		private String element;

		@Column(name = "parent_id")
		private Long parentId;

		public ElementEmbeddedId() {
		}

		public ElementEmbeddedId(String element, Long parentId) {
			this.parentId = parentId;
			this.element = element;
		}

		public String getElement() {
			return element;
		}

		public Long getParentId() {
			return parentId;
		}
	}

	@Entity
	@Table(name = "element_table")
	public static class Element {

		@EmbeddedId
		private ElementEmbeddedId id;

		private String name;

		public Element(ElementEmbeddedId id) {
			this.id = id;
		}

		public Element() {
		}

		public ElementEmbeddedId getId() {
			return id;
		}
	}

	@Entity
	@Table(name = "element_holder")
	public static class ElementHolder {

		@Id
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumns(
				value = {
						@JoinColumn(name = "parent_id", referencedColumnName = "parent_id"),
						@JoinColumn(name = "element", referencedColumnName = "element")
				}
		)
		private Element element;

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
	}

	@Entity
	@Table(name = "collection_holder")
	public static class CollectionHolder {
		@Id
		private Long id;

		private String name;

		@ElementCollection(fetch = FetchType.EAGER)
		@Column(name = "element", nullable = false)
		@CollectionTable(name = "elements", joinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id"))
		private Set<String> elements;

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
	}
}
