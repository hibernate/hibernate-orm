/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: AddressId.java 6979 2005-06-01 03:51:32Z oneovthafew $
package org.hibernate.test.typedmanytoone;
import java.io.Serializable;

/**
 * @author Gavin King
 */
public class AddressId implements Serializable {
	private String type;
	private String addressId;
	
	public AddressId(String type, String customerId) {
		this.addressId = customerId;
		this.type = type;
	}
	
	public AddressId() {}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getAddressId() {
		return addressId;
	}
	public void setAddressId(String customerId) {
		this.addressId = customerId;
	}
	public boolean equals(Object other) {
		if ( !(other instanceof AddressId) ) return false;
		AddressId add = (AddressId) other;
		return type.equals(add.type) && addressId.equals(add.addressId);
	}
	public int hashCode() {
		return addressId.hashCode() + type.hashCode();
	}
	
	public String toString() {
		return type + '#' + addressId;
	}

}
