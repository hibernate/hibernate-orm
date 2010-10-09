//$Id: FumCompositeID.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;

public class FumCompositeID implements java.io.Serializable {
	String string_;
	java.util.Date date_;
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
	public java.util.Date getDate() {
		return date_;
	}
	public void setDate(java.util.Date date_) {
		this.date_ = date_;
	}
	public short getShort() {
		return short_;
	}
	public void setShort(short short_) {
		this.short_ = short_;
	}
}







