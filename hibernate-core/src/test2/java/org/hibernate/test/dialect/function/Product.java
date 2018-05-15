/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.function;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 
 * @author Strong Liu <stliu@redhat.com>
 *
 */
public class Product {
	private Long id;
	private int length;
	private long weight;
	private BigDecimal price;
	private Date date;

	public Long getId() {
		return id;
	}

	public void setId( Long id ) {
		this.id = id;
	}

	public int getLength() {
		return length;
	}

	public void setLength( int length ) {
		this.length = length;
	}

	public long getWeight() {
		return weight;
	}

	public void setWeight( long weight ) {
		this.weight = weight;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice( BigDecimal price ) {
		this.price = price;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

}
