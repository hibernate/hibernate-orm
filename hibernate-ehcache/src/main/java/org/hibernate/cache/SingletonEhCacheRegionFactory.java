/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors. ÊAll third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.cache;

import java.util.Properties;

/**
 * Thin wrapper class around the within Ehcache-core packaged SingletonEhCacheRegionFactory.
 * It directly delegates to the wrapped instance, enabling user to upgrade the Ehcache-core version
 * by simply dropping in a new jar.
 *
 * @author Alex Snaps
 */
public final class SingletonEhCacheRegionFactory extends AbstractEhCacheRegionFactory {

	public SingletonEhCacheRegionFactory(Properties properties) {
		super(new net.sf.ehcache.hibernate.SingletonEhCacheRegionFactory(properties));
	}
}
