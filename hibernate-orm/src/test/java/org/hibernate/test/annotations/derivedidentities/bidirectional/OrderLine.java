/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
