/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
