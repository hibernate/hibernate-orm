package org.hibernate.test.collection.subclass.join.inverse;

import java.util.Set;

public class Payer {

	private Long payerId;
	
	private Set<EventPayer> eventPayers;
	
	public Long getPayerId() {
		return payerId;
	}
	
	public void setPayerId(Long payerId) {
		this.payerId = payerId;
	}
	
	public Set<EventPayer> getEventPayers() {
		return eventPayers;
	}
	
	public void setEventPayers(Set<EventPayer> eventPayers) {
		this.eventPayers = eventPayers;
	}
}
