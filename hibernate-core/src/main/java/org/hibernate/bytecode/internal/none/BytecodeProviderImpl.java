/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.none;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.cfg.AvailableSettings;

/**
 * This BytecodeProvider represents the "no-op" enhancer; mostly useful
 * as an optimisation when not needing any byte code optimisation applied,
 * for example when the entities have been enhanced at compile time.
 * Choosing this BytecodeProvider allows to exclude the bytecode enhancement
 * libraries from the runtime classpath, but is not compatible
 * with the option AvailableSettings#USE_REFLECTION_OPTIMIZER .
 *
 * @since 5.4
 */
public final class BytecodeProviderImpl implements BytecodeProvider {

	@Override
	public ProxyFactoryFactory getProxyFactoryFactory() {
		return new NoProxyFactoryFactory();
	}

	@Override
	public ReflectionOptimizer getReflectionOptimizer(
			Class clazz,
			String[] getterNames,
			String[] setterNames,
			Class[] types) {
		throw new HibernateException( "Using the ReflectionOptimizer is not possible when the configured BytecodeProvider is 'none'. Disable " + AvailableSettings.USE_REFLECTION_OPTIMIZER + " or use a different BytecodeProvider");
	}

	@Override
	public Enhancer getEnhancer(EnhancementContext enhancementContext) {
		return null;
	}
}
