/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.spi.ValueBinder;
import org.hibernate.type.descriptor.spi.ValueExtractor;

/**
 * @author Steve Ebersole
 */
public interface AllowableParameterType<T> extends ExpressableType<T> {
	ValueBinder getValueBinder();
	ValueExtractor getValueExtractor();

	// todo (6.0) - move this to ValueBinder?
	int getNumberOfJdbcParametersToBind();
}
