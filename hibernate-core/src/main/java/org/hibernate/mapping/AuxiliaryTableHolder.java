/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

/**
 * Something that can have an associated auxiliary table,
 * for example, an audit table or a temporal history table.
 *
 * @author Gavin King
 * @author Marco Belladelli
 * @see Stateful
 * @since 7.4
 */
public interface AuxiliaryTableHolder {

	Table getAuxiliaryTable();

	void setAuxiliaryTable(Table auxiliaryTable);

	Column getAuxiliaryColumn(String name);

	void addAuxiliaryColumn(String name, Column column);
}
