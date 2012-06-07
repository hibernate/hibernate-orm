//$Id: Folder.java 9538 2006-03-04 00:17:57Z steve.ebersole@jboss.com $
package org.hibernate.test.instrument.domain;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Gavin King
 */
public class Folder {
	private Long id;
	private String name;
	private Folder parent;
	private Collection subfolders = new ArrayList();
	private Collection documents = new ArrayList();

	public boolean nameWasread;
	
	/**
	 * @return Returns the id.
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		nameWasread = true;
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return Returns the documents.
	 */
	public Collection getDocuments() {
		return documents;
	}
	/**
	 * @param documents The documents to set.
	 */
	public void setDocuments(Collection documents) {
		this.documents = documents;
	}
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
	/**
	 * @return Returns the subfolders.
	 */
	public Collection getSubfolders() {
		return subfolders;
	}
	/**
	 * @param subfolders The subfolders to set.
	 */
	public void setSubfolders(Collection subfolders) {
		this.subfolders = subfolders;
	}
}
