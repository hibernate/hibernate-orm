/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
