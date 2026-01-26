/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

/**
 * Part of the boot model which is temporal.
 */
public interface Temporalized {
	void enableTemporal(Column startingColumn, Column endingColumn, boolean partitioned);

	Column getTemporalStartingColumn();

	Column getTemporalEndingColumn();

	boolean isTemporallyPartitioned();

	default boolean isTemporalized() {
		return getTemporalStartingColumn() != null;
	}
}
