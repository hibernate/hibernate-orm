/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

/**
 * Service contract for dealing with native queries.
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
 *
 * @deprecated (since 6.0) use {@link org.hibernate.query.spi.NativeQueryInterpreter}
 * instead.
 */
@Deprecated
public interface NativeQueryInterpreter extends org.hibernate.query.spi.NativeQueryInterpreter {
}
