//$Id: Contract.java 7222 2005-06-19 17:22:01Z oneovthafew $
package org.hibernate.test.immutable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Contract implements Serializable {
	
	private long id;
	private String customerName;
	private String type;
	private List variations;

	public Contract() {
		super();
	}

	public Contract(String customerName, String type) {
		this.customerName = customerName;
		this.type = type;
		variations = new ArrayList();
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List getVariations() {
		return variations;
	}

	public void setVariations(List variations) {
		this.variations = variations;
	}

}
