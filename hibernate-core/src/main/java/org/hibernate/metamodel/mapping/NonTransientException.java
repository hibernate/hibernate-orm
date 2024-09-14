/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping;

/**
 * Marker interface for exceptions thrown during mapping-model creation which
 * are not transient errors - they will never succeed
 *
 * @author Steve Ebersole
 */
public interface NonTransientException {
}
