/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.spi;

/**
 * Optional contract for a {@link org.hibernate.type.Type} which is aware
 * of its scoping to a {@link TypeConfiguration} and which receives access
 * to the {@code TypeConfiguration} to which it is scoped.
 * <p>
 * For additional information about scoping, see {@link TypeConfiguration}.
 *
 * @apiNote A {@code Type} which implements {@code TypeConfigurationAware}
 *          may not be scoped to more than one {@code TypeConfiguration}.
 *          The method {@link #getTypeConfiguration()} allows this rule
 *          to be enforced.
 *
 * @author Steve Ebersole
 */
public interface TypeConfigurationAware {
	TypeConfiguration getTypeConfiguration();
	void setTypeConfiguration(TypeConfiguration typeConfiguration);
}
