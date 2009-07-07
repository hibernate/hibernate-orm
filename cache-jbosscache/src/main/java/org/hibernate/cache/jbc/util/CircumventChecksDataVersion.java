/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
package org.hibernate.cache.jbc.util;

import org.hibernate.cache.CacheException;
import org.jboss.cache.config.Option;
import org.jboss.cache.optimistic.DataVersion;

/**
 * Used to signal to a DataVersionAdapter to simply not perform any checks. This
 * is currently needed for proper handling of remove() calls for entity cache
 * regions (we do not know the version info...).
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class CircumventChecksDataVersion implements DataVersion {

    private static final long serialVersionUID = 7996980646166032369L;

    public static final DataVersion INSTANCE = new CircumventChecksDataVersion();

    public static Option getInvocationOption() {
        Option option = new Option();
        option.setDataVersion(INSTANCE);
        return option;
    }

    public boolean newerThan(DataVersion dataVersion) {
        throw new CacheException("optimistic locking checks should never happen on CircumventChecksDataVersion");
    }

}
