//$Id: DataPoint.java 10223 2006-08-04 20:29:21Z steve.ebersole@jboss.com $
package org.hibernate.test.proxy;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Gavin King
 */
public class DataPoint implements Serializable {
	private long id;
	private BigDecimal x;
	private BigDecimal y;
	private String description;

	public DataPoint() {
	}

	public DataPoint(BigDecimal x, BigDecimal y, String description) {
		this.x = x;
		this.y = y;
		this.description = description;
	}

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
	 * @return Returns the id.
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @return Returns the x.
	 */
	public BigDecimal getX() {
		return x;
	}
	/**
	 * @param x The x to set.
	 */
	public void setX(BigDecimal x) {
		this.x = x;
	}
	/**
	 * @return Returns the y.
	 */
	public BigDecimal getY() {
		return y;
	}
	/**
	 * @param y The y to set.
	 */
	public void setY(BigDecimal y) {
		this.y = y;
	}
	
	void exception() throws Exception {
		throw new Exception("foo");
	}
}
