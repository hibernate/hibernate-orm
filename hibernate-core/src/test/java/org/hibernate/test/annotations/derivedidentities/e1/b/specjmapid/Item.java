/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.test.annotations.derivedidentities.e1.b.specjmapid;
import java.io.Serializable;
import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Version;


@NamedQueries({
		@NamedQuery(name = "Item.findByCategory",
				query = "SELECT i FROM Item i WHERE i.category=:category ORDER BY i.id")
})
@SuppressWarnings("serial")
@Entity
@Table(name = "O_ITEM")
public class Item implements Serializable {

	public static final String QUERY_BY_CATEGORY = "Item.findByCategory";

	@Id
	@Column(name = "I_ID")
	private String id;

	@Column(name = "I_NAME")
	private String name;

	@Column(name = "I_PRICE")
	private BigDecimal price;

	@Column(name = "I_DESC")
	private String description;

	@Column(name = "I_DISCOUNT")
	private BigDecimal discount;

	@Column(name = "I_CATEGORY")
	private int category;

	@Version
	@Column(name = "I_VERSION")
	int version;

	public String getId() {
		return id;
	}

	public void setId(String i) {
		id = i;
	}

	public int getCategory() {
		return category;
	}

	public void setCategory(int category) {
		this.category = category;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getDiscount() {
		return discount;
	}

	public void setDiscount(BigDecimal discount) {
		if ( discount.doubleValue() < 0 || discount.doubleValue() > 100.0 ) {
			throw new IllegalArgumentException(
					this + " discount " + discount
							+ " is invalid. Must be between 0.0 and 100.0"
			);
		}
		this.discount = discount;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public int getVersion() {
		return version;
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null || other.getClass() != this.getClass() ) {
			return false;
		}
		if ( other == this ) {
			return true;
		}
		return id.equals( ( ( Item ) other ).id );
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
