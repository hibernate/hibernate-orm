/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.convert;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Converts;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.CollectionPropertyHolder;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * Test for {@link CollectionPropertyHolder}.
 * Tests that {@link Converter}s are considered correctly for {@link ElementCollection}.
 */
@TestForIssue( jiraKey = "HHH-9495" )
public class ElementCollectionConversionTest {
	@Test
	public void testSimpleConvertUsage() throws MalformedURLException {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( TheEntity.class.getName() );
			}
		};

		final Map settings = new HashMap();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder(pu, settings).build();

		try {
         TheEntity entity = new TheEntity(1);

         EntityManager em = emf.createEntityManager();
			em.getTransaction().begin();
			em.persist( entity );
			em.getTransaction().commit();
			em.close();

			em = emf.createEntityManager();
			em.getTransaction().begin();
         TheEntity retrieved = em.find(TheEntity.class, 1);

         assertEquals(1, retrieved.getSet().size());
         assertEquals(new ValueType("set_value"), retrieved.getSet().iterator().next());
         assertEquals(1, retrieved.getMap().size());
         assertEquals(new ValueType("map_value"), retrieved.getMap().get(new ValueType("map_key")));

         em.getTransaction().commit();
			em.close();
		}
		finally {
			emf.close();
		}
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
         this.map.putIfAbsent(new ValueType("map_key"), new ValueType("map_value"));
      }

      public Set<ValueType> getSet() {
         return set;
      }

      public Map<ValueType, ValueType> getMap() {
         return map;
      }
   }
}
