/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

/**
 * MultiKeyLoader implementation based on a SQL ARRAY valued parameter
 *
 * @see SqlInPredicateMultiKeyLoader
 *
 * @author Steve Ebersole
 */
public interface SqlArrayMultiKeyLoader extends MultiKeyLoader {
}
