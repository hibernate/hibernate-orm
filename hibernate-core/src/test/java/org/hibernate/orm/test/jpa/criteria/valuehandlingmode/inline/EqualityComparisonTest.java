/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.valuehandlingmode.inline;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = {
				EqualityComparisonTest.Address.class,
				EqualityComparisonTest.Phone.class,
				EqualityComparisonTest.Product.class
		}
		, properties = @Setting(name = AvailableSettings.CRITERIA_VALUE_HANDLING_MODE, value = "inline")
)
public class EqualityComparisonTest {

	@Test
	public void testEqualityComparisonEntityConversion(EntityManagerFactoryScope scope) {
		Address address = new Address( "Street Id", "Fake Street", "Fake City", "Fake State", "Fake Zip" );
		scope.inTransaction(
				entityManager -> {
					Phone phone1 = new Phone( "1", "555", "0001", address );
					Phone phone2 = new Phone( "2", "555", "0002", address );
					Phone phone3 = new Phone( "3", "555", "0003", address );
					Phone phone4 = new Phone( "4", "555", "0004" );

					List<Phone> phones = new ArrayList<>( 3 );
					phones.add( phone1 );
					phones.add( phone2 );
					phones.add( phone3 );

					address.setPhones( phones );
					entityManager.persist( address );
					entityManager.persist( phone4 );
				}
		);

		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					JpaMetamodel mm = (JpaMetamodel) entityManager.getMetamodel();
					EntityType<Phone> Phone_ = mm.entity( Phone.class );

					CriteriaQuery<Phone> cquery = cb.createQuery( Phone.class );
					Root<Phone> phone = cquery.from( Phone.class );
					Predicate predicate = cb.equal(
							phone.get( Phone_.getSingularAttribute( "address", Address.class ) ),
							address
					);
					cquery.where( predicate );
					List<Phone> results = entityManager.createQuery( cquery ).getResultList();

					assertEquals( 3, results.size() );
				}
		);
	}

	@Test
	public void testEqualityComparisonLiteralConversion(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					JpaMetamodel mm = (JpaMetamodel) entityManager.getMetamodel();

					CriteriaQuery<Integer> cquery = cb.createQuery( Integer.class );
					Root<Product> product = cquery.from( Product.class );
					EntityType<Product> Product_ = mm.entity( Product.class );

					cquery.select(
							cb.toInteger(
									product.get(
											Product_.getSingularAttribute( "quantity", Integer.class ) )
							)
					);

					SqmComparisonPredicate predicate = (SqmComparisonPredicate) cb.equal(
							product.get( Product_.getSingularAttribute( "partNumber", Long.class ) ),
							373767373
					);
					Assert.assertEquals( Long.class, predicate.getLeftHandExpression().getJavaType() );
					cquery.where( predicate );
					entityManager.createQuery( cquery ).getResultList();

					predicate = (SqmComparisonPredicate) cb.ge(
							cb.length( product.get( Product_.getSingularAttribute( "name", String.class ) ) ),
							4L
					);
					Assert.assertEquals( Integer.class, predicate.getLeftHandExpression().getJavaType() );
					cquery.where( predicate );
					entityManager.createQuery( cquery ).getResultList();
				}
		);
	}

	@Entity
	@Table(name = "ADDRESS")
	public static class Address implements java.io.Serializable {
		private String id;
		private String street;
		private String city;
		private String state;
		private String zip;
		private List<Phone> phones = new ArrayList<>();

		public Address() {
		}

		public Address(String id, String street, String city, String state, String zip) {
			this.id = id;
			this.street = street;
			this.city = city;
			this.state = state;
			this.zip = zip;
		}

		public Address(
				String id,
				String street,
				String city,
				String state,
				String zip,
				List<Phone> phones) {
			this.id = id;
			this.street = street;
			this.city = city;
			this.state = state;
			this.zip = zip;
			this.phones = phones;
		}

		@Id
		@Column(name = "ID")
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Column(name = "STREET")
		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		@Column(name = "CITY")
		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		@Column(name = "STATE")
		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}

		@Column(name = "ZIP")
		public String getZip() {
			return zip;
		}

		public void setZip(String zip) {
			this.zip = zip;
		}

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "address")
		@OrderColumn
		public List<Phone> getPhones() {
			return phones;
		}

		public void setPhones(List<Phone> phones) {
			this.phones = phones;
		}
	}

	@Entity
	@Table(name = "PHONE_TABLE")
	public static class Phone implements java.io.Serializable {
		public enum Type {LAND_LINE, CELL, FAX, WORK, HOME}

		private String id;
		private String area;
		private String number;
		private Address address;
		private Set<Phone.Type> types;

		public Phone() {
		}

		public Phone(String v1, String v2, String v3) {
			id = v1;
			area = v2;
			number = v3;
		}

		public Phone(String v1, String v2, String v3, Address v4) {
			id = v1;
			area = v2;
			number = v3;
			address = v4;
		}

		@Id
		@Column(name = "ID")
		public String getId() {
			return id;
		}

		public void setId(String v) {
			id = v;
		}

		@Column(name = "AREA")
		public String getArea() {
			return area;
		}

		public void setArea(String v) {
			area = v;
		}

		@Column(name = "PHONE_NUMBER")
		public String getNumber() {
			return number;
		}

		public void setNumber(String v) {
			number = v;
		}

		@ManyToOne
		@JoinColumn(name = "FK_FOR_ADDRESS")
		public Address getAddress() {
			return address;
		}

		public void setAddress(Address a) {
			address = a;
		}

		@ElementCollection
		public Set<Phone.Type> getTypes() {
			return types;
		}

		public void setTypes(Set<Phone.Type> types) {
			this.types = types;
		}
	}

	@Entity
	@Table(name = "PRODUCT_TABLE")
	@SecondaryTable(name = "PRODUCT_DETAILS", pkJoinColumns = @PrimaryKeyJoinColumn(name = "ID"))
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "PRODUCT_TYPE", discriminatorType = DiscriminatorType.STRING)
	@DiscriminatorValue("Product")
	public static class Product {
		private String id;
		private String name;
		private double price;
		private float rating;
		private int quantity;
		private long partNumber;
		private BigInteger someBigInteger;
		private BigDecimal someBigDecimal;
		private String wareHouse;
		private ShelfLife shelfLife;

		public Product() {
		}

		public Product(String id, String name, double price, int quantity, long partNumber) {
			this.id = id;
			this.name = name;
			this.price = price;
			this.quantity = quantity;
			this.partNumber = partNumber;
		}

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

		public float getRating() {
			return rating;
		}

		public void setRating(float rating) {
			this.rating = rating;
		}

		public BigInteger getSomeBigInteger() {
			return someBigInteger;
		}

		public void setSomeBigInteger(BigInteger someBigInteger) {
			this.someBigInteger = someBigInteger;
		}

		@Column(precision = 10, scale = 3)
		public BigDecimal getSomeBigDecimal() {
			return someBigDecimal;
		}

		public void setSomeBigDecimal(BigDecimal someBigDecimal) {
			this.someBigDecimal = someBigDecimal;
		}

		@Column(name = "WHOUSE", nullable = true, table = "PRODUCT_DETAILS")
		public String getWareHouse() {
			return wareHouse;
		}

		public void setWareHouse(String v) {
			this.wareHouse = v;
		}

		@Embedded
		@AttributeOverrides({
				@AttributeOverride(name = "inceptionDate",
						column = @Column(name = "INCEPTION", nullable = true)),
				@AttributeOverride(name = "soldDate",
						column = @Column(name = "SOLD", nullable = true))
		})
		public ShelfLife getShelfLife() {
			return shelfLife;
		}

		public void setShelfLife(ShelfLife v) {
			this.shelfLife = v;
		}
	}

	@Embeddable
	public static class ShelfLife implements java.io.Serializable {
		private Date inceptionDate;
		private Date soldDate;

		public ShelfLife() {
		}

		public ShelfLife(Date inceptionDate, Date soldDate) {
			this.inceptionDate = inceptionDate;
			this.soldDate = soldDate;
		}

		@Basic
		public Date getInceptionDate() {
			return inceptionDate;
		}

		public void setInceptionDate(Date inceptionDate) {
			this.inceptionDate = inceptionDate;
		}

		@Basic
		public Date getSoldDate() {
			return soldDate;
		}

		public void setSoldDate(Date soldDate) {
			this.soldDate = soldDate;
		}
	}


}
