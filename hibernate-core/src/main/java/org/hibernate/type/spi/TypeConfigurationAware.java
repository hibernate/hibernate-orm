/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
