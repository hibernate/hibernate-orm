/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.constraint;


import org.hibernate.metamodel.mapping.SelectableMappings;

/**
 * @author Steve Ebersole
 */
public interface Constraint {
	String getConstrainedTableName();
	SelectableMappings getConstrainedColumnMappings();
	boolean isNullable();
	boolean isDeferrable();
}
