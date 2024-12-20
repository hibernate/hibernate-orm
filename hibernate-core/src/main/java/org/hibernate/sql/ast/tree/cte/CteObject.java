/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.cte;

/**
 * An object that is part of a WITH clause.
 *
 * @author Christian Beikov
 */
public interface CteObject {

	String getName();

}
