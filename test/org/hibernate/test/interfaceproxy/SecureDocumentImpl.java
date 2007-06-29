//$Id$
package org.hibernate.test.interfaceproxy;

/**
 * @author Gavin King
 */
public class SecureDocumentImpl extends DocumentImpl implements SecureDocument {
	private byte permissionBits;
	private String owner;
	/**
	 * @return Returns the owner.
	 */
	public String getOwner() {
		return owner;
	}
	/**
	 * @param owner The owner to set.
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}
	/**
	 * @return Returns the permissionBits.
	 */
	public byte getPermissionBits() {
		return permissionBits;
	}
	/**
	 * @param permissionBits The permissionBits to set.
	 */
	public void setPermissionBits(byte permissionBits) {
		this.permissionBits = permissionBits;
	}
}
