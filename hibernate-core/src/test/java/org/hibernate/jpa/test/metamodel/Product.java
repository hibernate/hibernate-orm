/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "PRODUCT_TABLE")
@SecondaryTable(name = "PRODUCT_DETAILS", pkJoinColumns = @PrimaryKeyJoinColumn(name = "ID"))
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "PRODUCT_TYPE", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("Product")
public class Product implements java.io.Serializable {
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

	@Column( precision = 10, scale = 3)
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
