//$Id: BuyNow.java 3890 2004-06-03 16:31:32Z steveebersole $
package org.hibernate.auction;

/**
 * @author Gavin King
 */
public class BuyNow extends Bid {
	public boolean isBuyNow() {
		return true;
	}
	public String toString() {
		return super.toString() + " (buy now)";
	}
}
