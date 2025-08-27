/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

/**
 * MultiKeyLoader implementation based on SQL IN predicate
 *
 * @see SqlArrayMultiKeyLoader
 *
 * @author Steve Ebersole
 */
public interface SqlInPredicateMultiKeyLoader extends MultiKeyLoader {
}
