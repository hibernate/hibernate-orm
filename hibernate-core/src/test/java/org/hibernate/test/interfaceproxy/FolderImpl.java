//$Id: FolderImpl.java 4407 2004-08-22 01:20:08Z oneovthafew $
package org.hibernate.test.interfaceproxy;


/**
 * @author Gavin King
 */
public class FolderImpl extends ItemImpl implements Folder {
	private Folder parent;
	/**
	 * @return Returns the parent.
	 */
	public Folder getParent() {
		return parent;
	}
	/**
	 * @param parent The parent to set.
	 */
	public void setParent(Folder parent) {
		this.parent = parent;
	}
}
