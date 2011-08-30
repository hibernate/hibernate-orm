//$Id: Document.java 7772 2005-08-05 23:03:46Z oneovthafew $
package org.hibernate.test.lazycache;
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
	private Date lastTextModification;
	
	public Document(String name, String summary, String text) {
		lastTextModification = new Date();
		this.name = name;
		upperCaseName = name.toUpperCase();
		this.summary = summary;
		this.text = text;
	}
	
	Document() {}
	
	public Date getLastTextModification() {
		return lastTextModification;
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
