/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

/**
 * Optional contract for {@link org.hibernate.type.Type}s which would like to be part of the scoping
 * process of the {@link TypeConfiguration}, that is, to receive access to the {@code TypeConfiguration}
 * to which they are scoped. For additional information on {@code TypeConfiguration} scoping, see
 * {@link TypeConfiguration}.
 * <p>
 * Note that it is illegal for a Type to implement TypeConfigurationAware and at the same time
 * be scoped to more than one TypeConfiguration.  Hibernate will enforce this internally
 * which is why {@link #getTypeConfiguration()} is exposed here.
 *
 * @author Steve Ebersole
 */
public interface TypeConfigurationAware {
	TypeConfiguration getTypeConfiguration();
	void setTypeConfiguration(TypeConfiguration typeConfiguration);
}
