package org.hibernate.test.sql.hand;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Product {
	
	static public class ProductId implements Serializable {
		String orgid;
		String productnumber;
		public String getProductnumber() {
			return productnumber;
		}
		public void setProductnumber(String ordernumber) {
			this.productnumber = ordernumber;
		}
		public String getOrgid() {
			return orgid;
		}
		public void setOrgid(String orgid) {
			this.orgid = orgid;
		}
		
		
	}
	
	ProductId productId;
	
	String name;

	Person person;
	
	Set orders = new HashSet();
	
	public Set getOrders() {
		return orders;
	}
	
	public void setOrders(Set orders) {
		this.orders = orders;
	}
	public Person getPerson() {
		return person;
	}
	
	public void setPerson(Person person) {
		this.person = person;
	}
	public ProductId getProductId() {
		return productId;
	}

	public void setProductId(ProductId orderId) {
		this.productId = orderId;
	}

	public String getName() {
		return name;
	}

	public void setName(String product) {
		this.name = product;
	}

}
