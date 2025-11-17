/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11841")
@EnversTest
@Jpa(xmlMappings = {
		"mappings/collections/Category.hbm.xml",
		"mappings/collections/Item.hbm.xml"
})
public class EntityMapCompositeElementTest {

	private Long categoryId;
	private Long itemId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager-> {
			final Item item = new Item( "The Item" );
			entityManager.persist( item );

			final Category category = new Category( "The Category" );
			category.setDescription( "The description" );
			category.setValue( item, new Value( "The Value", 4711L ) );
			category.setText( item, "The text" );
			entityManager.persist( category );

			this.categoryId = category.getId();
			this.itemId = item.getId();
		} );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-11841", reason = "Reverted fix in HHH-12018 and will be fixed in HHH-12043")
	public void testRevisionHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final AuditReader reader = AuditReaderFactory.get( em );

			AuditQuery categoryQuery = reader.createQuery().forRevisionsOfEntity( Category.class, false, true )
					.addOrder( AuditEntity.revisionProperty( "timestamp" ).asc() )
					.add( AuditEntity.id().eq( categoryId ) );

			@SuppressWarnings( "unchecked" )
			List<Object[]> history = (List<Object[]>) categoryQuery.getResultList();
			assertNotNull( history );
			assertEquals( 1, history.size() );

			final Item item = em.find( Item.class, itemId );
			final Category category = (Category) reader.createQuery().forEntitiesAtRevision( Category.class, 1 )
					.add( AuditEntity.property( "id" ).eq( this.categoryId ) )
					.setMaxResults( 1 )
					.getSingleResult();

			assertEquals( "The Category", category.getName() );
			assertEquals( "The description", category.getDescription() );
			assertEquals( "The text", category.getText( item ) );

			final Value value = category.getValue( item );
			assertEquals( "The Value", value.getText() );
			assertEquals( Long.valueOf( 4711L ), value.getNumberValue() );
		} );
	}

	@Audited
	public static class Category {
		private Long id;
		private String name;
		private String description;
		private Map<Item, String> textItem = new HashMap<>();
		private Map<Item, Value> categoryItem = new HashMap<>();

		Category() {

		}

		Category(String name) {
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

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Map<Item, String> getTextItem() {
			return textItem;
		}

		public void setTextItem(Map<Item, String> textItem) {
			this.textItem = textItem;
		}

		public Map<Item, Value> getCategoryItem() {
			return categoryItem;
		}

		public void setCategoryItem(Map<Item, Value> categoryItem) {
			this.categoryItem = categoryItem;
		}

		public void setValue(Item key, Value value) {
			this.categoryItem.put( key, value );
		}

		public Value getValue(Item key) {
			return this.categoryItem.get( key );
		}

		public void setText(Item key, String value) {
			this.textItem.put( key, value );
		}

		public String getText(Item key) {
			return this.textItem.get( key );
		}
	}

	@Audited
	public static class Item {
		private Long id;
		private String name;

		Item() {

		}

		Item(String name) {
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

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Item item = (Item) o;

			return id != null ? id.equals( item.id ) : item.id == null;
		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}
	}

	public static class Value implements Serializable {
		private String text;
		private Long numberValue;

		Value() {

		}

		Value(String text, Long numberValue) {
			this.text = text;
			this.numberValue = numberValue;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public Long getNumberValue() {
			return numberValue;
		}

		public void setNumberValue(Long numberValue) {
			this.numberValue = numberValue;
		}
	}
}
