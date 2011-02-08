package org.hibernate.test.cut;


/**
 * @author Rob.Hasselbaum
 *
 */
public class MutualFund {
	
	private Long id;
	private MonetoryAmount holdings;
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}

	public MonetoryAmount getHoldings() {
		return holdings;
	}

	public void setHoldings(MonetoryAmount holdings) {
		this.holdings = holdings;
	}

}
