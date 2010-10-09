// $Id: Order.java 4222 2004-08-10 05:19:46Z steveebersole $
package org.hibernate.test.filter;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Steve Ebersole
 */
public class Order {
	private Long id;
	private String region;
	private Date placementDate;
	private Date fulfillmentDate;
	private Salesperson salesperson;
	private String buyer;
	private List lineItems = new ArrayList();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public Date getPlacementDate() {
		return placementDate;
	}

	public void setPlacementDate(Date placementDate) {
		this.placementDate = placementDate;
	}

	public Date getFulfillmentDate() {
		return fulfillmentDate;
	}

	public void setFulfillmentDate(Date fulfillmentDate) {
		this.fulfillmentDate = fulfillmentDate;
	}

	public Salesperson getSalesperson() {
		return salesperson;
	}

	public void setSalesperson(Salesperson salesperson) {
		this.salesperson = salesperson;
	}

	public String getBuyer() {
		return buyer;
	}

	public void setBuyer(String buyer) {
		this.buyer = buyer;
	}

	public List getLineItems() {
		return lineItems;
	}

	protected void setLineItems(List lineItems) {
		this.lineItems = lineItems;
	}

	public LineItem addLineItem(Product product, long quantity) {
		return LineItem.generate(this, getLineItems().size(), product, quantity);
	}

	public void removeLineItem(LineItem item) {
		removeLineItem( item.getSequence() );
	}

	public void removeLineItem(int sequence) {
		getLineItems().remove(sequence);
	}
}
