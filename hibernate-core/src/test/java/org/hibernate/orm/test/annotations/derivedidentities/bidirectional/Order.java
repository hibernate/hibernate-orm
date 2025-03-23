/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.bidirectional;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.processing.Exclude;

@SuppressWarnings("serial")
@Entity
@Exclude
@Table(name = "orders")
public class Order implements Serializable
{
	@Id
	@GeneratedValue
	private Long id;

	@Column(name = "name", length = 20)
	private String name;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
	private Set<OrderLine> lineItems = new HashSet<OrderLine>();

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Set<OrderLine> getLineItems()
	{
		return lineItems;
	}

	public void setLineItems(Set<OrderLine> lineItems)
	{
		this.lineItems = lineItems;
	}

	public void addLineItem(Product product, Integer amount)
	{
		OrderLine part = new OrderLine(this, product, amount);
		lineItems.add(part);
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((id == null) ? 0 : id.hashCode());
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
		final Order other = (Order) obj;
		if (id == null)
		{
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
