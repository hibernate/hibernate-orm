/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

/**
 * Unified contract for things that can contain a SqmFromClause.
 *
 * @author Steve Ebersole
 */
public interface SqmFromClauseContainer {
	/**
	 * Obtains this container's SqmFromClause.
	 *
	 * @return This container's SqmFromClause.
	 */
	SqmFromClause getFromClause();
}
