package org.hibernate.test.collection.subclass.join.inverse;

public class EventPayer extends Event {

	private Payer payer;
	
	public Payer getPayer() {
		return payer;
	}
	
	public void setPayer(Payer payer) {
		this.payer = payer;
	}
}
