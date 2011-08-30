//$Id: Product.java 4806 2004-11-25 14:37:00Z steveebersole $
package org.hibernate.test.cid;
import java.math.BigDecimal;

/**
 * @author Gavin King
 */
public class Product {
	private String productId;
	private String description;
	private BigDecimal price;
	private int numberAvailable;
	private int numberOrdered;
	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description The description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return Returns the numberAvailable.
	 */
	public int getNumberAvailable() {
		return numberAvailable;
	}
	/**
	 * @param numberAvailable The numberAvailable to set.
	 */
	public void setNumberAvailable(int numberAvailable) {
		this.numberAvailable = numberAvailable;
	}
	/**
	 * @return Returns the numberOrdered.
	 */
	public int getNumberOrdered() {
		return numberOrdered;
	}
	/**
	 * @param numberOrdered The numberOrdered to set.
	 */
	public void setNumberOrdered(int numberOrdered) {
		this.numberOrdered = numberOrdered;
	}
	/**
	 * @return Returns the productId.
	 */
	public String getProductId() {
		return productId;
	}
	/**
	 * @param productId The productId to set.
	 */
	public void setProductId(String productId) {
		this.productId = productId;
	}
	/**
	 * @return Returns the price.
	 */
	public BigDecimal getPrice() {
		return price;
	}
	/**
	 * @param price The price to set.
	 */
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
}
