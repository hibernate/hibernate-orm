/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Inc. or third-party contributors as
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
package org.hibernate.ejb.metamodel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "LINEITEM_TABLE")
public class LineItem implements java.io.Serializable {
	private String id;
	private int quantity;
	private Order order;
	private Product product;

	public LineItem() {
	}

	public LineItem(String v1, int v2, Order v3, Product v4) {
		id = v1;
		quantity = v2;
		order = v3;
		product = v4;
	}

	public LineItem(String v1, int v2) {
		id = v1;
		quantity = v2;
	}

	@Id
	@Column(name = "ID")
	public String getId() {
		return id;
	}

	public void setId(String v) {
		id = v;
	}

	@Column(name = "QUANTITY")
	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int v) {
		quantity = v;
	}

	@ManyToOne
	@JoinColumn(name = "FK1_FOR_ORDER_TABLE")
	public Order getOrder() {
		return order;
	}

	public void setOrder(Order v) {
		order = v;
	}

	@ManyToOne
	@JoinColumn(name = "FK_FOR_PRODUCT_TABLE")
	public Product getProduct() {
		return product;
	}

	public void setProduct(Product v) {
		product = v;
	}
}