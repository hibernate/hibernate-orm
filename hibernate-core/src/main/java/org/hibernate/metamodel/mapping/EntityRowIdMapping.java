/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

/**
 * Mapping of a row-id
 *
 * @see org.hibernate.annotations.RowId
 */
public interface EntityRowIdMapping extends BasicValuedModelPart, VirtualModelPart, SelectableMapping {
	String getRowIdName();
}
