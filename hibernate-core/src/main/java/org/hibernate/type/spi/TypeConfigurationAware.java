/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

/**
 * Optional contract for Types that would like to be part of the scoping process of the
 * TypeConfiguration, specifically to receive access to the TypeConfiguration it is scoped
 * to.  For additional information on TypeConfiguration scoping, see {@link TypeConfiguration}
 * <p/>
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
