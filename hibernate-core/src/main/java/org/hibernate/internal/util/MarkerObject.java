/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

/**
 * Marker objects used throughout Hibernate to represent special sentinel values.
 * These are used for reference-equality checks (==) to indicate special states
 * like "no row found", "unfetched collection", etc.
 *
 * <p>Implemented as an enum to ensure proper singleton behavior across serialization
 * and deserialization, fixing issues where marker object identity was lost after
 * deserialization (e.g., in clustered environments or session serialization scenarios).
 *
 * @see <a href="https://hibernate.atlassian.net/browse/HHH-9414">HHH-9414</a>
 */
public enum MarkerObject {
	/**
	 * Marker indicating that no row was returned from the database.
	 */
	NO_ROW("NO_ROW"),

	/**
	 * Marker used in proxy implementations to signal that the actual
	 * implementation method should be invoked.
	 */
	INVOKE_IMPLEMENTATION("INVOKE_IMPLEMENTATION"),

	/**
	 * Marker indicating an unknown state in persistent collections.
	 */
	UNKNOWN("UNKNOWN"),

	/**
	 * Marker indicating a collection that has not yet been fetched from the database.
	 */
	UNFETCHED_COLLECTION("UNFETCHED COLLECTION"),

	/**
	 * Marker representing a null discriminator value.
	 */
	NULL_DISCRIMINATOR("<null discriminator>"),

	/**
	 * Marker representing a not-null discriminator value.
	 */
	NOT_NULL_DISCRIMINATOR("<not null discriminator>");

	private final String name;

	MarkerObject(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
