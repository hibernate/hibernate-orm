/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.custom;

/**
 * @author Steve Ebersole
 */
public class PayloadWrapper implements Comparable<PayloadWrapper> {
	private String payload;

	public PayloadWrapper() {
		this( null );
	}

	public PayloadWrapper(String payload) {
		this.payload = payload;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	@Override
	public int compareTo(PayloadWrapper other) {
		if ( getPayload() == null ) {
			return -1;
		}

		if ( other == null || other.getPayload() == null ) {
			return 1;
		}

		return getPayload().compareTo( other.getPayload() );
	}
}
