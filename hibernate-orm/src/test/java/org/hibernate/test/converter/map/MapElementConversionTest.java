/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter.map;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.AttributeConverter;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Converter;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-8529" )
public class MapElementConversionTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Customer.class, ColorTypeConverter.class };
	}

	@Test
	public void testElementCollectionConversion() {
		Session session = openSession();
		session.getTransaction().begin();
		Customer customer = new Customer( 1 );
		customer.colors.put( "eyes", ColorType.BLUE );
		session.persist( customer );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		assertEquals( 1, session.get( Customer.class, 1 ).colors.size() );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		customer = session.get( Customer.class, 1 );
		session.delete( customer );
		session.getTransaction().commit();
		session.close();
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
