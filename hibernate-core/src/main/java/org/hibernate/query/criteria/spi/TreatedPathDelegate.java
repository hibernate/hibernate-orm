/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * Marker interface for "treated path" references
 *
 * @author Steve Ebersole
 */
public interface TreatedPathDelegate<T> extends PathImplementor<T> {
	PathImplementor<? super T> getUntreatedPath();
}
