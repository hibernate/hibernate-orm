/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.annotations.Remove;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Marker interface for any types that are allowed as function returns
 *
 * @author Steve Ebersole
 */
public interface AllowableFunctionReturnType<T> extends ExpressableType<T> {

	SqlExpressableType getSqlExpressableType(TypeConfiguration typeConfiguration);

	@Remove
	SqlExpressableType getSqlExpressableType();

}
