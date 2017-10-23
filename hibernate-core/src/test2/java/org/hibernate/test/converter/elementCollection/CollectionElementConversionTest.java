/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter.elementCollection;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Converter;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
@TestForIssue(jiraKey = "HHH-8529")
public class CollectionElementConversionTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Customer.class, ColorConverter.class };
	}

	@Test
	public void testElementCollectionConversion() {
		Session session = openSession();
		session.getTransaction().begin();
		Customer customer = new Customer();
		customer.id = 1;
		customer.set = new HashSet<Color>();
		customer.set.add(Color.RED);
		customer.set.add(Color.GREEN);
		customer.set.add(Color.BLUE);
		customer.map = new HashMap<Color, Status>();
		customer.map.put(Color.RED, Status.INACTIVE);
		customer.map.put(Color.GREEN, Status.ACTIVE);
		customer.map.put(Color.BLUE, Status.PENDING);
		session.persist(customer);
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		assertEquals(customer.set, session.get(Customer.class, 1).set);
		assertEquals(customer.map, session.get(Customer.class, 1).map);
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		customer = session.get(Customer.class, 1);
		session.delete(customer);
		session.getTransaction().commit();
		session.close();
	}

	@Entity
	@Table(name = "Customer")
	public static class Customer {
		@Id
		private Integer id;
		@ElementCollection
		@Column(name = "`set`")
		private Set<Color> set;
		@ElementCollection
		@Enumerated(EnumType.STRING)
		private Map<Color, Status> map;
	}

	public static class Color {
		public static Color RED = new Color(0xFF0000);
		public static Color GREEN = new Color(0x00FF00);
		public static Color BLUE = new Color(0x0000FF);

		private final int rgb;

		public Color(int rgb) {
			this.rgb = rgb;
		}

		@Override
		public int hashCode() {
			return this.rgb;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Color && ((Color) obj).rgb == this.rgb;
		}
	}

	public static enum Status {
		ACTIVE,
		INACTIVE,
		PENDING
	}

	@Converter(autoApply = true)
	public static class ColorConverter implements AttributeConverter<Color, String> {
		@Override
		public String convertToDatabaseColumn(Color attribute) {
			return attribute == null ? null : Integer.toString(attribute.rgb, 16);
		}

		@Override
		public Color convertToEntityAttribute(String dbData) {
			return dbData == null ? null : new Color(Integer.parseInt(dbData, 16));
		}
	}
}
