//$Id: Permission.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.collection.original;


/**
 * @author Gavin King
 */
public class Permission {
	private String type;
	Permission() {}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Permission(String type) {
		this.type = type;
	}
	public boolean equals(Object that) {
		if ( !(that instanceof Permission) ) return false;
		Permission p = (Permission) that;
		return this.type.equals(p.type);
	}
	public int hashCode() {
		return type.hashCode();
	}
}
