
package org.hibernate.jpa.test.criteria.fetchscroll;

import jakarta.persistence.*;

@Embeddable
public class OrderLineId extends OrderId {

	private Long lineNumber;
	
	@Column(name = "LINE_NUMBER", nullable = false, updatable = false)
	public Long getLineNumber() {
		return lineNumber;
	}
	
	public void setLineNumber(Long lineNumber) {
		this.lineNumber = lineNumber;
	}
	
}
