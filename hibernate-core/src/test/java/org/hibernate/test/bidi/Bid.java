//$Id: Bid.java 5733 2005-02-14 15:56:06Z oneovthafew $
package org.hibernate.test.bidi;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Gavin King
 */
public class Bid {
	private Long id;
	private Auction item;
	private BigDecimal amount;
	private boolean successful;
	private Date datetime; 

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	public Auction getItem() {
		return item;
	}
	public void setItem(Auction item) {
		this.item = item;
	}
	public boolean isSuccessful() {
		return successful;
	}
	public void setSuccessful(boolean successful) {
		this.successful = successful;
	}
	public Date getDatetime() {
		return datetime;
	}
	public void setDatetime(Date datetime) {
		this.datetime = datetime;
	}
}
