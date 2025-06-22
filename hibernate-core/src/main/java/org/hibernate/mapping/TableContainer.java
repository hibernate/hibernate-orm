/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

/**
 * Container for Table and Join reference
 *
 * @author Steve Ebersole
 */
public interface TableContainer {
	Table findTable(String name);
	Table getTable(String name);
	Join findSecondaryTable(String name);
	Join getSecondaryTable(String name);
}
