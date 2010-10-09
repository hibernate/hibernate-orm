//$Id: ProductLine.java 4460 2004-08-29 12:04:14Z oneovthafew $
package org.hibernate.test.batchfetch;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class ProductLine {

	private String id;
	private String description;
	private Set models = new HashSet();
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Set getModels() {
		return models;
	}
	public void setModels(Set models) {
		this.models = models;
	}
}
