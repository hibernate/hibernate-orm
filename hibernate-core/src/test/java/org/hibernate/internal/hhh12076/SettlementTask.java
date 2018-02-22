package org.hibernate.internal.hhh12076;

public class SettlementTask extends Task<Settlement> {
	private static final long serialVersionUID = 1L;

	private Settlement _linked;

	public Settlement getLinked() {
		return _linked;
	}

	public void setLinked(Settlement settlement) {
		_linked = settlement;
	}

}
