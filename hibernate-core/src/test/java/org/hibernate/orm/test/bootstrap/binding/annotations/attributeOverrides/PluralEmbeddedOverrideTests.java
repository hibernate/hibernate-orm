/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.attributeOverrides;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.type.BasicType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.util.SchemaUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				PluralEmbeddedOverrideTests.TypeValue.class,
				PluralEmbeddedOverrideTests.AggregatedTypeValue.class
		}
)
@ServiceRegistry
@SessionFactory
public class PluralEmbeddedOverrideTests {

	@SuppressWarnings("rawtypes")
	@Test
	@JiraKey(value = "HHH-8630")
	public void testModel(DomainModelScope scope) {
		final PersistentClass entityBinding = scope.getDomainModel().getEntityBinding( AggregatedTypeValue.class.getName() );
		final Property attributesBinding = entityBinding.getProperty( "attributes" );
		final org.hibernate.mapping.Map attributesMap = (org.hibernate.mapping.Map) attributesBinding.getValue();

		final SimpleValue mapKey = assertTyping( SimpleValue.class, attributesMap.getIndex() );
		final BasicType mapKeyType = assertTyping( BasicType.class, mapKey.getType() );
		assertEquals( String.class, mapKeyType.getReturnedClass() );

		// let's also make sure the @MapKeyColumn got applied
		assertThat( mapKey.getColumnSpan(), is( 1 ) );
		final org.hibernate.mapping.Column mapKeyColumn = assertTyping(
				org.hibernate.mapping.Column.class,
				mapKey.getSelectables().get( 0 )
		);
		assertThat( mapKeyColumn.getName(), equalTo( "attribute_name" ) );

		assertTrue( SchemaUtil.isTablePresent( "AGG_TYPE", scope.getDomainModel() ) );
		Assertions.assertTrue( SchemaUtil.isColumnPresent( "AGG_TYPE", "content_type", scope.getDomainModel() ) );
		Assertions.assertTrue( SchemaUtil.isColumnPresent( "AGG_TYPE", "content_value", scope.getDomainModel() ) );

		assertTrue( SchemaUtil.isTablePresent( "ATTRIBUTES", scope.getDomainModel() ) );
		Assertions.assertTrue( SchemaUtil.isColumnPresent( "ATTRIBUTES", "attribute_type", scope.getDomainModel() ) );
		Assertions.assertTrue( SchemaUtil.isColumnPresent( "ATTRIBUTES", "attribute_value", scope.getDomainModel() ) );
	}

	@Test
	public void testOperations(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final AggregatedTypeValue loaded = session.get( AggregatedTypeValue.class, 1 );
					final AggregatedTypeValue queried = session.createQuery("from AggregatedTypeValue v", AggregatedTypeValue.class ).getSingleResult();

					assertThat( loaded, sameInstance( queried ) );

					assertThat( loaded.content.type, is( "notes" ) );
					assertThat( loaded.content.value, is( "Something worth noting" ) );

					assertThat( loaded.attributes, notNullValue() );
					assertThat( loaded.attributes.size(), is( 2 ) );

					final TypeValue attr1 = loaded.attributes.get( "attr1" );
					assertThat( attr1.type, is( "attribute" ) );
					assertThat( attr1.value, is( "The first" ) );

					final TypeValue attr2 = loaded.attributes.get( "attr2" );
					assertThat( attr2.type, is( "attribute" ) );
					assertThat( attr2.value, is( "The second" ) );
				}
		);
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final AggregatedTypeValue aggregate = new AggregatedTypeValue( 1 );

					aggregate.content = new TypeValue( "notes", "Something worth noting" );

					aggregate.attributes = new HashMap<>();
					aggregate.attributes.put( "attr1", new TypeValue( "attribute", "The first" ) );
					aggregate.attributes.put( "attr2", new TypeValue( "attribute", "The second" ) );

					session.persist( aggregate );
				}
		);
	}

	@AfterEach
	public void cleanupTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Embeddable
	public static class TypeValue {
		String type;

		@Column(columnDefinition = "TEXT")
		String value;

		/**
		 * Used by Hibernate via reflection
		 */
		@SuppressWarnings("unused")
		TypeValue() {
		}

		public TypeValue(String type, String value) {
			this.type = type;
			this.value = value;
		}
	}

	@Entity( name = "AggregatedTypeValue" )
	@Table(name = "AGG_TYPE")
	public static class AggregatedTypeValue {
		@Id
		Integer id;

		@Embedded
		@AttributeOverrides({
				@AttributeOverride(name = "type", column = @Column(name = "content_type")),
				@AttributeOverride(name = "value", column = @Column(name = "content_value"))
		})
		TypeValue content;

		@CollectionTable(name = "ATTRIBUTES")
		@ElementCollection(fetch = FetchType.EAGER)
		@MapKeyColumn(name = "attribute_name")
		@AttributeOverrides({
				@AttributeOverride(name = "value.type", column = @Column(name = "attribute_type")),
				@AttributeOverride(name = "value.value", column = @Column(name = "attribute_value"))
		})
		Map<String, TypeValue> attributes;

		/**
		 * Used by Hibernate via reflection
		 */
		@SuppressWarnings("unused")
		AggregatedTypeValue() {
		}

		public AggregatedTypeValue(Integer id) {
			this.id = id;
		}
	}

}
