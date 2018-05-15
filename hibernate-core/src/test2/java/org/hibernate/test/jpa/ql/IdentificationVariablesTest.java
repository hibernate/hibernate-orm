/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.ql;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.Session;

import org.hibernate.test.jpa.AbstractJPATest;
import org.junit.Test;

/**
 * Mainly a test for testing compliance with the fact that "identification variables" (aliases) need to
 * be treated as case-insensitive according to JPA.
 *
 * @author Steve Ebersole
 */
public class IdentificationVariablesTest extends AbstractJPATest {

	@Test
	public void testUsageInSelect() {
		Session s = openSession();
		s.createQuery( "select I from Item i" ).list();
		s.close();
	}

	@Test
	public void testUsageInPath() {
		Session s = openSession();
		s.createQuery( "select I from Item i where I.name = 'widget'" ).list();
		s.close();
	}

	@Test
	public void testMixedTckUsage() {
		Session s = openSession();
		s.createQuery( "Select DISTINCT OBJECT(P) from Product p where P.quantity < 10" ).list();
		s.close();
	}

	@Test
	public void testUsageInJpaInCollectionSyntax() {
		Session s = openSession();
		s.createQuery( "SELECT DISTINCT object(i) FROM Item I, IN(i.parts) ip where ip.stockNumber = '123'" ).list();
		s.close();
	}

	@Test
	public void testUsageInDistinct() {
		Session s = openSession();
		s.createQuery( "select distinct(I) from Item i" ).list();
		s.close();
	}

	@Test
	public void testUsageInSelectObject() {
		Session s = openSession();
		s.createQuery( "select OBJECT(I) from Item i" ).list();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Product.class };
	}

	@Entity( name = "Product")
	@Table( name = "PROD" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "PRODUCT_TYPE", discriminatorType = DiscriminatorType.STRING )
	@DiscriminatorValue( "Product" )
	public static class Product {
		private String id;
		private String name;
		private double price;
		private int quantity;
		private long partNumber;

		@Id
		@Column(name = "ID")
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Column(name = "NAME")
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Column(name = "PRICE")
		public double getPrice() {
			return price;
		}

		public void setPrice(double price) {
			this.price = price;
		}

		@Column(name = "QUANTITY")
		public int getQuantity() {
			return quantity;
		}

		public void setQuantity(int v) {
			this.quantity = v;
		}

		@Column(name = "PNUM")
		public long getPartNumber() {
			return partNumber;
		}

		public void setPartNumber(long v) {
			this.partNumber = v;
		}
	}
}
