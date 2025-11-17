/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade.multilevel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "MIDDLE")
public class Middle {
	@Id
	private Long id;
	@ManyToOne
	private Top top;

	@OneToOne(cascade = { CascadeType.ALL })
	@JoinColumn(name = "BOTTOM_ID")
	private Bottom bottom;

	private Middle() {

	}

	public Middle(Long i) {
		this.id = i;
	}

	Long getId() {
		return id;
	}

	void setId(Long id) {
		this.id = id;
	}

	Top getTop() {
		return top;
	}

	void setTop(Top top) {
		this.top = top;
	}

	Bottom getBottom() {
		return bottom;
	}

	void setBottom(Bottom bottom) {
		this.bottom = bottom;
		bottom.setMiddle(this);
	}
}
