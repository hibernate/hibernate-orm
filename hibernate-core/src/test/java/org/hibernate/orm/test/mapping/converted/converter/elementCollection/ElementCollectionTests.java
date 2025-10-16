/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.elementCollection;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.model.internal.CollectionPropertyHolder;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converts;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for {@link CollectionPropertyHolder}.
 *
 * Tests that {@link jakarta.persistence.AttributeConverter}s are considered correctly for {@link jakarta.persistence.ElementCollection}.
 *
 * @author Markus Heiden
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-9495" )
@DomainModel(
		annotatedClasses = ElementCollectionTests.TheEntity.class
)
@SessionFactory
public class ElementCollectionTests {

	@Test
	public void testSimpleConvertUsage(DomainModelScope modelScope, SessionFactoryScope sfScope) throws MalformedURLException {
		// first some assertions of the metamodel
		PersistentClass entityBinding = modelScope.getDomainModel().getEntityBinding( TheEntity.class.getName() );
		assertNotNull( entityBinding );

		Property setAttributeBinding = entityBinding.getProperty( "set" );
		Collection setBinding = (Collection) setAttributeBinding.getValue();
		assertTyping( ConvertedBasicTypeImpl.class, setBinding.getElement().getType() );

		Property mapAttributeBinding = entityBinding.getProperty( "map" );
		IndexedCollection mapBinding = (IndexedCollection) mapAttributeBinding.getValue();
		assertTyping( ConvertedBasicTypeImpl.class, mapBinding.getIndex().getType() );
		assertTyping( ConvertedBasicTypeImpl.class, mapBinding.getElement().getType() );

		// now lets try to use the model, integration-testing-style!
		TheEntity entity = new TheEntity( 1 );

		sfScope.inTransaction(
				(session) -> {
					session.persist( entity );
				}
		);

		sfScope.inTransaction(
				(session) -> {
					TheEntity retrieved = (TheEntity) session.getReference( TheEntity.class, 1 );
					assertEquals( 1, retrieved.getSet().size() );
					assertEquals( new ValueType( "set_value" ), retrieved.getSet().iterator().next() );
					assertEquals( 1, retrieved.getMap().size() );
					assertEquals( new ValueType( "map_value" ), retrieved.getMap().get( new ValueType( "map_key" ) ) );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	/**
	 * Non-serializable value type.
	 */
	public static class ValueType {
		@Column(name = "val")
		private final String value;

		public ValueType(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof ValueType &&
					value.equals(((ValueType) o).value);
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}
	}

	/**
	 * Converter for {@link ValueType}.
	 */
	public static class ValueTypeConverter implements AttributeConverter<ValueType, String> {
		@Override
		public String convertToDatabaseColumn(ValueType type) {
			return type.getValue();
		}

		@Override
		public ValueType convertToEntityAttribute(String type) {
			return new ValueType(type);
		}
	}

	/**
	 * Entity holding element collections.
	 */
	@Entity( name = "TheEntity" )
	@Table(name = "entity")
	public static class TheEntity {
		@Id
		public Integer id;

		/**
		 * Element set with converter.
		 */
		@Convert( converter = ValueTypeConverter.class )
		@ElementCollection(fetch = FetchType.LAZY)
		@CollectionTable(name = "entity_set", joinColumns = @JoinColumn(name = "entity_id", nullable = false))
		@Column(name = "val", nullable = false)
		public Set<ValueType> set = new HashSet<ValueType>();

		/**
		 * Element map with converters.
		 */
		@Converts({
				@Convert(attributeName = "key", converter = ValueTypeConverter.class),
				@Convert(attributeName = "value", converter = ValueTypeConverter.class)
		})
		@ElementCollection(fetch = FetchType.LAZY)
		@CollectionTable(name = "entity_map", joinColumns = @JoinColumn(name = "entity_id", nullable = false))
		@MapKeyColumn(name = "map_key", nullable = false)
		@Column(name = "val", nullable = false)
		public Map<ValueType, ValueType> map = new HashMap<ValueType, ValueType>();

		public TheEntity() {
		}

		public TheEntity(Integer id) {
			this.id = id;
			this.set.add(new ValueType("set_value"));
			this.map.put( new ValueType( "map_key" ), new ValueType( "map_value" ) );
		}

		public Set<ValueType> getSet() {
			return set;
		}

		public Map<ValueType, ValueType> getMap() {
			return map;
		}
	}
}
