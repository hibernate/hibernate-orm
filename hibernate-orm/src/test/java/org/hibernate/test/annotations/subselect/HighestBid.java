/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.annotations.subselect;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

/**
 * @author Sharath Reddy
 *
 */
@Entity
@Subselect("select Item.name as name, max(Bid.amount) as amount from Item, Bid where Bid.itemId = Item.id group by Item.name")
@Synchronize({"Item", "Bid"})
public class HighestBid {
	 
	private String name;
	private double amount;
	
	@Id
	public String getName() {
		return name;
	}
	public void setName(String val) {
		this.name = val;
	}
	public double getAmount() {
		return amount;
	}
	public void setAmount(double amount) {
		this.amount = amount;
	}
	
	
	
}
