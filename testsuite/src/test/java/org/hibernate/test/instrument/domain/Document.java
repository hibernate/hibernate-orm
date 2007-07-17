//$Id: Document.java 9538 2006-03-04 00:17:57Z steve.ebersole@jboss.com $
package org.hibernate.test.instrument.domain;

import java.util.Date;

/**
 * @author Gavin King
 */
public class Document {
	private Long id;
	private String name;
	private String upperCaseName;
	private String summary;
	private String text;
	private Owner owner;
	private Folder folder;
	private Date lastTextModification = new Date();
	/**
	 * @return Returns the folder.
	 */
	public Folder getFolder() {
		return folder;
	}
	/**
	 * @param folder The folder to set.
	 */
	public void setFolder(Folder folder) {
		this.folder = folder;
	}
	/**
	 * @return Returns the owner.
	 */
	public Owner getOwner() {
		return owner;
	}
	/**
	 * @param owner The owner to set.
	 */
	public void setOwner(Owner owner) {
		this.owner = owner;
	}
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
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return Returns the summary.
	 */
	public String getSummary() {
		return summary;
	}
	/**
	 * @param summary The summary to set.
	 */
	public void setSummary(String summary) {
		this.summary = summary;
	}
	/**
	 * @return Returns the text.
	 */
	public String getText() {
		return text;
	}
	/**
	 * @param text The text to set.
	 */
	private void setText(String text) {
		this.text = text;
	}
	/**
	 * @return Returns the upperCaseName.
	 */
	public String getUpperCaseName() {
		return upperCaseName;
	}
	/**
	 * @param upperCaseName The upperCaseName to set.
	 */
	public void setUpperCaseName(String upperCaseName) {
		this.upperCaseName = upperCaseName;
	}
	
	public void updateText(String newText) {
		if ( !newText.equals(text) ) {
			this.text = newText;
			lastTextModification = new Date();
		}
	}
	
}
