//$Id: Item.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.cache;


/**
 * @author Gavin King
 */
public class Item {
	private Long id;
	private String name;
	private String description;
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
