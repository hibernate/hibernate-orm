package org.hibernate.test.idprops;
import java.io.Serializable;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class LineItemPK implements Serializable {
	private Order order;
	private String productCode;

	public LineItemPK() {
	}

	public LineItemPK(Order order, String productCode) {
		this.order = order;
		this.productCode = productCode;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public String getProductCode() {
		return productCode;
	}

	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		LineItemPK that = ( LineItemPK ) o;

		if ( !order.equals( that.order ) ) {
			return false;
		}
		if ( !productCode.equals( that.productCode ) ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = order.hashCode();
		result = 31 * result + productCode.hashCode();
		return result;
	}
}
