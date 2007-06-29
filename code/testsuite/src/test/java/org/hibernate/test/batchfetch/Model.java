//$Id: Model.java 4460 2004-08-29 12:04:14Z oneovthafew $
package org.hibernate.test.batchfetch;

/**
 * @author Gavin King
 */
public class Model {
	private String id;
	private String name;
	private String description;
	private ProductLine productLine;
	
	Model() {}
	
	public Model(ProductLine pl) {
		this.productLine = pl;
		pl.getModels().add(this);
	}
	
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
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public ProductLine getProductLine() {
		return productLine;
	}
	public void setProductLine(ProductLine productLine) {
		this.productLine = productLine;
	}
}
