/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import org.infinispan.commands.module.ExtendedModuleCommandFactory;
import org.infinispan.commands.module.ModuleCommandExtensions;
import org.infinispan.commands.module.ModuleCommandInitializer;

/**
 * Command extensions for second-level cache use case
 *
 * @author Galder Zamarreño
 * @since 4.0
 */
public class CacheCommandExtensions implements ModuleCommandExtensions {
	final CacheCommandFactory cacheCommandFactory = new CacheCommandFactory();
	final CacheCommandInitializer cacheCommandInitializer = new CacheCommandInitializer();

	@Override
	public ExtendedModuleCommandFactory getModuleCommandFactory() {
		return cacheCommandFactory;
	}

	@Override
	public ModuleCommandInitializer getModuleCommandInitializer() {
		return cacheCommandInitializer;
	}

}
