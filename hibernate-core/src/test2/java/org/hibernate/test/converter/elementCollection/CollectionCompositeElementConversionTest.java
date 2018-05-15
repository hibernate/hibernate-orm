/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter.elementCollection;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.AttributeConverter;
import javax.persistence.CollectionTable;
import javax.persistence.Converter;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
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
public class CollectionCompositeElementConversionTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Disguise.class, ColorTypeConverter.class };
	}

	@Test
	public void testElementCollectionConversion() {
		Session session = openSession();
		session.getTransaction().begin();
		Disguise disguise = new Disguise( 1 );
		disguise.traits.add( new Traits( ColorType.BLUE, ColorType.RED ) );
		session.persist( disguise );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		assertEquals( 1, session.get( Disguise.class, 1 ).traits.size() );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		disguise = session.get( Disguise.class, 1 );
		session.delete( disguise );
		session.getTransaction().commit();
		session.close();
	}

	@Entity( name = "Disguise" )
	@Table( name = "DISGUISE" )
	public static class Disguise {
		@Id
		private Integer id;

		@ElementCollection(fetch = FetchType.EAGER)
		@CollectionTable(
				name = "DISGUISE_TRAIT",
				joinColumns = @JoinColumn(name = "DISGUISE_FK", nullable = false)
		)
		private Set<Traits> traits = new HashSet<Traits>();

		public Disguise() {
		}

		public Disguise(Integer id) {
			this.id = id;
		}
	}

	@Embeddable
	public static class Traits {
		public ColorType eyeColor;
		public ColorType hairColor;

		public Traits() {
		}

		public Traits(
				ColorType eyeColor,
				ColorType hairColor) {
			this.eyeColor = eyeColor;
			this.hairColor = hairColor;
		}
	}

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
