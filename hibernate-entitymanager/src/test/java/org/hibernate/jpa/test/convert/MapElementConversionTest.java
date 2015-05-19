/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.convert;

import javax.persistence.AttributeConverter;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Converter;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-8529" )
public class MapElementConversionTest extends BaseUnitTestCase {
	@Test
	public void testElementCollectionConversion() {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( Customer.class.getName(), ColorTypeConverter.class.getName() );
			}
		};

		final Map settings = new HashMap();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();
		try {
			EntityManager em = emf.createEntityManager();
			em.getTransaction().begin();
			Customer customer = new Customer( 1 );
			customer.colors.put( "eyes", ColorType.BLUE );
			em.persist( customer );
			em.getTransaction().commit();
			em.close();

			em = emf.createEntityManager();
			em.getTransaction().begin();
			assertEquals( 1, em.find( Customer.class, 1 ).colors.size() );
			em.getTransaction().commit();
			em.close();

			em = emf.createEntityManager();
			em.getTransaction().begin();
			customer = em.find( Customer.class, 1 );
			em.remove( customer );
			em.getTransaction().commit();
			em.close();
		}
		finally {
			emf.close();
		}
	}

	@Entity( name = "Customer" )
	@Table( name = "CUST" )
	public static class Customer {
		@Id
		private Integer id;

		@ElementCollection(fetch = FetchType.EAGER)
		@CollectionTable( name = "cust_color", joinColumns = @JoinColumn( name = "cust_fk" ) )
		@MapKeyColumn( name = "color_key" )
		@Column(name = "color", nullable = false)
		private Map<String,ColorType> colors = new HashMap<String,ColorType>();

		public Customer() {
		}

		public Customer(Integer id) {
			this.id = id;
		}
	}


	// an enum-like class (converters are technically not allowed to apply to enums)
	public static class ColorType {
		public static ColorType BLUE = new ColorType( "blue" );
		public static ColorType RED = new ColorType( "red" );
		public static ColorType YELLOW = new ColorType( "yellow" );

		private final String color;

		public ColorType(String color) {
			this.color = color;
		}

		public String toExternalForm() {
			return color;
		}

		public static ColorType fromExternalForm(String color) {
			if ( BLUE.color.equals( color ) ) {
				return BLUE;
			}
			else if ( RED.color.equals( color ) ) {
				return RED;
			}
			else if ( YELLOW.color.equals( color ) ) {
				return YELLOW;
			}
			else {
				throw new RuntimeException( "Unknown color : " + color );
			}
		}
	}

	@Converter( autoApply = true )
	public static class ColorTypeConverter implements AttributeConverter<ColorType, String> {

		@Override
		public String convertToDatabaseColumn(ColorType attribute) {
			return attribute == null ? null : attribute.toExternalForm();
		}

		@Override
		public ColorType convertToEntityAttribute(String dbData) {
			return ColorType.fromExternalForm( dbData );
		}
	}
}
