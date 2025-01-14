/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
 *
 * @see BindableType#resolveExpressible(BindingContext)
 * @see org.hibernate.query.sqm.SqmExpressible#resolveExpressible(BindingContext)
 * @see org.hibernate.query.sqm.produce.function.ArgumentsValidator#validate(java.util.List, String, BindingContext)
 * @see org.hibernate.query.sqm.internal.TypecheckUtil
 */
@Incubating
public interface BindingContext {
	JpaMetamodel getJpaMetamodel();

	MappingMetamodel getMappingMetamodel();

	TypeConfiguration getTypeConfiguration();
}
