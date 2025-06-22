/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.fetchscroll;

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
