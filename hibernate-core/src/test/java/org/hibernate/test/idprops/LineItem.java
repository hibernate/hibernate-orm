package org.hibernate.test.idprops;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class LineItem {
	private LineItemPK pk;
	private int quantity;
	private String id;

	public LineItem() {
	}

	public LineItem(LineItemPK pk, int quantity) {
		this.pk = pk;
		this.quantity = quantity;
		this.pk.getOrder().getLineItems().add( this );
	}

	public LineItem(Order order, String productCode, int quantity) {
		this( new LineItemPK( order, productCode ), quantity );
	}

	public LineItemPK getPk() {
		return pk;
	}

	public void setPk(LineItemPK pk) {
		this.pk = pk;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
