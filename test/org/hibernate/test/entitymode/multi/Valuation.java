// $Id: Valuation.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.entitymode.multi;

import java.util.Date;

/**
 * Implementation of Valuation.
 *
 * @author Steve Ebersole
 */
public class Valuation {
	private Long id;
	private Stock stock;
	private Date valuationDate;
	private Double value;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Stock getStock() {
		return stock;
	}

	public void setStock(Stock stock) {
		this.stock = stock;
	}

	public Date getValuationDate() {
		return valuationDate;
	}

	public void setValuationDate(Date valuationDate) {
		this.valuationDate = valuationDate;
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}
}
