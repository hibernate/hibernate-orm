/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

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
