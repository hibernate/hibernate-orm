/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

/**
 * Part of the boot model which is temporal.
 */
public interface Temporalized {
	void enableTemporal(Column rowStartColumn, Column rowEndColumn, boolean partitioned);

	Table getMainTable();

	Table getTemporalTable();

	void setTemporalTable(Table table);

	Column getTemporalStartingColumn();

	Column getTemporalEndingColumn();

	boolean isTemporallyPartitioned();

	default boolean isTemporalized() {
		return getTemporalStartingColumn() != null;
	}
}
