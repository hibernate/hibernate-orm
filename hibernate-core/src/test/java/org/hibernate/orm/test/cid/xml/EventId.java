/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cid.xml;

import java.util.Objects;

/**
 * Composite identifier mapped as an {@code <embeddable>} in XML.
 */
public class EventId  {
	private int region;
	private int sequence;

	public EventId() {
	}

	public EventId(int region, int sequence) {
		this.region = region;
		this.sequence = sequence;
	}

	public int getRegion() {
		return region;
	}

	public void setRegion(int region) {
		this.region = region;
	}

	public int getSequence() {
		return sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof EventId that) ) {
			return false;
		}
		return region == that.region && sequence == that.sequence;
	}

	@Override
	public int hashCode() {
		return Objects.hash( region, sequence );
	}
}
