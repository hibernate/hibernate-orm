/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.type.spi.TypeConfiguration;

/**
 * Any basic-typed ValueMapping - e.g. a basic-valued singular attribute or a
 * basic-valued collection element
 *
 * todo (6.0) : better to use {@link org.hibernate.metamodel.relational.Identifier} instead to handle quoting?
 *
 * todo (6.0) : expose {@link org.hibernate.metamodel.model.convert.spi.BasicValueConverter}?
 * 		- Or just handle internal to impl?
 *
 * @author Steve Ebersole
 */
public interface BasicValuedMapping extends ValueMapping, SqlExpressable {
	@Override
	default int getJdbcTypeCount(TypeConfiguration typeConfiguration) {
		return 1;
	}
}
