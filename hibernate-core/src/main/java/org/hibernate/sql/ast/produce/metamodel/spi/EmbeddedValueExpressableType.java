/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public interface EmbeddedValueExpressableType<T> extends ExpressableType<T>, AllowableParameterType<T> {
	EmbeddedTypeDescriptor getEmbeddedDescriptor();

	default int getNumberOfJdbcParametersForRestriction() {
		return getEmbeddedDescriptor().getNumberOfJdbcParametersForRestriction();
	}
}
