/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.convert.spi;

import org.hibernate.query.sqm.SqmExpressable;

/**
 * Describes a part of the domain model to which a value converter can be applied
 *
 * @author Steve Ebersole
 */
public interface ConvertibleValueMapping<O> extends SqmExpressable<O> {
	/**
	 * Get the value converter associated with this value mapping.  May
	 * return {@code null}
	 */
	<R> BasicValueConverter<O,R> getValueConverter();
}
