/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.valuegen.ValueGenerationStrategy;

/**
 * @author Steve Ebersole
 */
public interface NonIdPersistentAttribute<O,J> extends PersistentAttributeDescriptor<O,J>, StateArrayContributor<J> {
	@Override
	default Class<J> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	default ValueGenerationStrategy getValueGenerationStrategy() {
		throw new NotYetImplementedFor6Exception();
	}
}
