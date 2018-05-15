/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.hand;
import java.io.Serializable;

public class Order {
	
	static public class OrderId implements Serializable {
		String orgid;
		String ordernumber;
		public String getOrdernumber() {
			return ordernumber;
		}
		public void setOrdernumber(String ordernumber) {
			this.ordernumber = ordernumber;
		}
		public String getOrgid() {
			return orgid;
		}
		public void setOrgid(String orgid) {
			this.orgid = orgid;
		}
		
		
	}
	
	OrderId orderId;
	
	Product product;

	Person person;
	
	public Person getPerson() {
		return person;
	}
	
	public void setPerson(Person person) {
		this.person = person;
	}
	public OrderId getOrderId() {
		return orderId;
	}

	public void setOrderId(OrderId orderId) {
		this.orderId = orderId;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

}
