/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;



public class FumCompositeID implements java.io.Serializable {
	String string_;
	short short_;
	public boolean equals(Object other) {
		FumCompositeID that = (FumCompositeID) other;
		return this.string_.equals(that.string_) && this.short_==that.short_;
	}
	public int hashCode() {
		return string_.hashCode();
	}
	public String getString() {
		return string_;
	}
	public void setString(String string_) {
		this.string_ = string_;
	}
	public short getShort() {
		return short_;
	}
	public void setShort(short short_) {
		this.short_ = short_;
	}
}
