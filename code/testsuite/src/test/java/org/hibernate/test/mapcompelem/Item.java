//$Id: Item.java 6236 2005-03-29 03:20:23Z oneovthafew $
package org.hibernate.test.mapcompelem;

/**
 * @author Gavin King
 */
public class Item {

	private String code;
	private Product product;
	
	
	Item() {}
	public Item(String code, Product p) {
		this.code = code;
		this.product = p;
	}

	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public Product getProduct() {
		return product;
	}
	
	public void setProduct(Product product) {
		this.product = product;
	}
	
}
