/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 *
 */
package org.hibernate.cache;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Settings;

import java.util.Properties;

/**
 * Defines a factory for query cache instances.  These factories are responsible for
 * creating individual QueryCache instances.
 *
 * @author Steve Ebersole
 */
public interface QueryCacheFactory {

	public QueryCache getQueryCache(
	        String regionName,
	        UpdateTimestampsCache updateTimestampsCache,
			Settings settings,
	        Properties props) 
	throws HibernateException;

}
