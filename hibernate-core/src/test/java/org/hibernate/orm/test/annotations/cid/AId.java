/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;
import java.io.Serializable;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;


/**
 * @author Artur Legan
 */
@Embeddable
public class AId implements Serializable {

	@OneToOne
	@JoinColumn( name = "bid", nullable = false )
	private B b;

	@OneToOne
	@JoinColumn( name = "cid", nullable = false )
	private C c;


	public B getB() {
		return b;
	}

	public void setB(B b) {
		this.b = b;
	}

	public C getC() {
		return c;
	}

	public void setC(C c) {
		this.c = c;
	}
}
