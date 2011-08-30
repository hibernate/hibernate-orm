//$Id: Customer.java 4806 2004-11-25 14:37:00Z steveebersole $
package org.hibernate.test.cid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * @author Gavin King
 */
public class Customer {
	private String customerId;
	private String name;
	private String address;
	private List orders = new ArrayList();
	/**
	 * @return Returns the address.
	 */
	public String getAddress() {
		return address;
	}
	/**
	 * @param address The address to set.
	 */
	public void setAddress(String address) {
		this.address = address;
	}
	/**
	 * @return Returns the customerId.
	 */
	public String getCustomerId() {
		return customerId;
	}
	/**
	 * @param customerId The customerId to set.
	 */
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return Returns the orders.
	 */
	public List getOrders() {
		return orders;
	}
	/**
	 * @param orders The orders to set.
	 */
	public void setOrders(List orders) {
		this.orders = orders;
	}

	public Order generateNewOrder(BigDecimal total) {
		Order order = new Order(this);
		order.setOrderDate( new GregorianCalendar() );
		order.setTotal( total );

		return order;
	}
}
