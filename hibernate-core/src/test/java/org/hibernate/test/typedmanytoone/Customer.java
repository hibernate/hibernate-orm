//$Id: Customer.java 6979 2005-06-01 03:51:32Z oneovthafew $
package org.hibernate.test.typedmanytoone;
import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Customer implements Serializable {

	private String name;
	private String customerId;
	private Address billingAddress;
	private Address shippingAddress;

	public Address getBillingAddress() {
		return billingAddress;
	}
	public void setBillingAddress(Address billingAddress) {
		this.billingAddress = billingAddress;
	}
	public String getCustomerId() {
		return customerId;
	}
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Address getShippingAddress() {
		return shippingAddress;
	}
	public void setShippingAddress(Address shippingAddress) {
		this.shippingAddress = shippingAddress;
	}
}
