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
