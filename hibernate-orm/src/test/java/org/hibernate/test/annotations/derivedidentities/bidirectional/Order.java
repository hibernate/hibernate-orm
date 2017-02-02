/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.bidirectional;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@SuppressWarnings("serial")
@Entity
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
