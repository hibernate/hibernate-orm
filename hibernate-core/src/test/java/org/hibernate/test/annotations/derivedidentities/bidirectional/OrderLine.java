/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.derivedidentities.bidirectional;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@SuppressWarnings("serial")
@Entity
@Table(name = "order_line")
// @IdClass(OrderLinePK.class)
public class OrderLine implements Serializable
{
	@Id
	@ManyToOne(targetEntity = Order.class)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;
	@Id
	@ManyToOne(targetEntity = Product.class)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;
	@Column(name = "amount")
	private Integer amount;
	
	public OrderLine()
	{
		super();
	}

	public OrderLine(Order order, Product product, Integer amount)
	{
		this();
		this.order = order;
		this.product = product;
		this.amount = amount;
	}

	public Product getProduct()
	{
		return product;
	}

	public void setProduct(Product product)
	{
		this.product = product;
	}

	public Order getOrder()
	{
		return order;
	}

	public void setOrder(Order order)
	{
		this.order = order;
	}

	public Integer getAmount()
	{
		return amount;
	}

	public void setAmount(Integer amount)
	{
		this.amount = amount;
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((amount == null) ? 0 : amount.hashCode());
		result = PRIME * result + ((order == null) ? 0 : order.hashCode());
		result = PRIME * result + ((product == null) ? 0 : product.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final OrderLine other = (OrderLine) obj;
		if (amount == null)
		{
			if (other.amount != null)
				return false;
		} else if (!amount.equals(other.amount))
			return false;
		if (order == null)
		{
			if (other.order != null)
				return false;
		} else if (!order.equals(other.order))
			return false;
		if (product == null)
		{
			if (other.product != null)
				return false;
		} else if (!product.equals(other.product))
			return false;
		return true;
	}
}
