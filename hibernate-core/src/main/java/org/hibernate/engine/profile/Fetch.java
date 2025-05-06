/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.profile;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;

/**
 * Models an individual fetch override within a {@link FetchProfile}.
 *
 * @author Steve Ebersole
 */
public class Fetch {
	private final Association association;
	private final FetchStyle method;
	private final FetchTiming timing;

	/**
	 * Constructs a {@link Fetch}.
	 *
	 * @param association The association to be fetched
	 * @param method How to fetch it
	 */
	public Fetch(Association association, FetchStyle method, FetchTiming timing) {
		this.association = association;
		this.method = method;
		this.timing = timing;
	}

	/**
	 * The association to which the fetch style applies.
	 */
	public Association getAssociation() {
		return association;
	}

	/**
	 * The fetch method to be applied to the association.
	 */
	public FetchStyle getMethod() {
		return method;
	}

	/**
	 * The fetch timing to be applied to the association.
	 */
	public FetchTiming getTiming() {
		return timing;
	}

	@Override
	public String toString() {
		return "Fetch[" + method + "{" + association.getRole() + "}]";
	}
}
