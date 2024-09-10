/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.Incubating;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A context within which a {@link BindableType} can be resolved
 * to an instance of {@link org.hibernate.query.sqm.SqmExpressible}.
 *
 * @author Gavin King
 *
 * @since 7
 */
@Incubating
public interface BindingContext {
	JpaMetamodel getJpaMetamodel();

	MappingMetamodel getMappingMetamodel();

	default TypeConfiguration getTypeConfiguration() {
		return getJpaMetamodel().getTypeConfiguration();
	}
}
