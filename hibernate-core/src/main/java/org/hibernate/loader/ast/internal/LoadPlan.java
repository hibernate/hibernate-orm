/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.sql.exec.spi.JdbcSelect;

/**
 * Common contract for SQL AST based loading
 *
 * @author Steve Ebersole
 */
public interface LoadPlan {
	/**
	 * The thing being loaded
	 */
	Loadable getLoadable();

	/**
	 * The part of the thing being loaded used to restrict which loadables get loaded
	 */
	ModelPart getRestrictivePart();

	/**
	 * The JdbcSelect for the load
	 */
	JdbcSelect getJdbcSelect();
}
