/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.hbm2x.hbm2hbmxml.FormulaTest;

public class AddressId {
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
		if ( !(other instanceof AddressId add) ) return false;
        return type.equals(add.type) && addressId.equals(add.addressId);
	}
	public int hashCode() {
		return addressId.hashCode() + type.hashCode();
	}
	
	public String toString() {
		return type + '#' + addressId;
	}

}
