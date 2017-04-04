/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.subselect;
import javax.persistence.Entity;
import javax.persistence.Id;


/**
 * @author Sharath Reddy
 */
@Entity
public class Bid {

	private int id;
	private long itemId;
	private double amount;
	
	@Id
    public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public long getItemId() {
		return itemId;
	}
	public void setItemId(long itemId) {
		this.itemId = itemId;
	}
	public double getAmount() {
		return amount;
	}
	public void setAmount(double val) {
		this.amount = val;
	}
	
	
	
}
