/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.converter;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Converts;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test for {@link org.hibernate.cfg.CollectionPropertyHolder}.
 *
 * Tests that {@link javax.persistence.AttributeConverter}s are considered correctly for {@link javax.persistence.ElementCollection}.
 *
 * @author Markus Heiden
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-9495" )
public class ElementCollectionTests extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { TheEntity.class };
	}

	@Test
	public void testSimpleConvertUsage() throws MalformedURLException {
		// first some assertions of the metamodel
		PersistentClass entityBinding = metadata().getEntityBinding( TheEntity.class.getName() );
		assertNotNull( entityBinding );

		Property setAttributeBinding = entityBinding.getProperty( "set" );
		Collection setBinding = (Collection) setAttributeBinding.getValue();
		assertTyping( AttributeConverterTypeAdapter.class, setBinding.getElement().getType() );

		Property mapAttributeBinding = entityBinding.getProperty( "map" );
		IndexedCollection mapBinding = (IndexedCollection) mapAttributeBinding.getValue();
		assertTyping( AttributeConverterTypeAdapter.class, mapBinding.getIndex().getType() );
		assertTyping( AttributeConverterTypeAdapter.class, mapBinding.getElement().getType() );

		// now lets try to use the model, integration-testing-style!
		TheEntity entity = new TheEntity(1);

		Session s = openSession();
		s.beginTransaction();
		s.save( entity );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		TheEntity retrieved = (TheEntity) s.load( TheEntity.class, 1 );
		assertEquals( 1, retrieved.getSet().size() );
		assertEquals(new ValueType("set_value"), retrieved.getSet().iterator().next());
		assertEquals(1, retrieved.getMap().size());
		assertEquals(new ValueType("map_value"), retrieved.getMap().get(new ValueType("map_key")));
		s.delete( retrieved );
		s.getTransaction().commit();
		s.close();
	}

	/**
	 * Non-serializable value type.
	 */
	public static class ValueType {
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
		@Column(name = "value", nullable = false)
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
		@MapKeyColumn(name = "key", nullable = false)
		@Column(name = "value", nullable = false)
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
