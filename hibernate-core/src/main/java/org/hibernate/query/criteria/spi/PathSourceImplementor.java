/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.query.criteria.JpaPathSource;

/**
 * SPI-level contract fpr {@link org.hibernate.query.criteria.JpaPathSource}
 * implementations
 *
 * @author Steve Ebersole
 */
public interface PathSourceImplementor<T> extends JpaPathSource<T> {
	@Override
	ManagedTypeDescriptor<T> getManagedType();
}
