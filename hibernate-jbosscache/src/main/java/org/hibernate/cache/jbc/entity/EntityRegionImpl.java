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
package org.hibernate.cache.jbc.entity;

import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.config.Configuration.NodeLockingScheme;
import org.jboss.cache.notifications.annotation.CacheListener;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.jbc.TransactionalDataRegionAdapter;
import org.hibernate.cache.jbc.access.PutFromLoadValidator;

/**
 * Defines the behavior of the entity cache regions for JBossCache.
 * 
 * @author Steve Ebersole
 */
@CacheListener
public class EntityRegionImpl extends TransactionalDataRegionAdapter implements EntityRegion {

    public static final String TYPE = "ENTITY";
    
    private boolean optimistic;

    public EntityRegionImpl(Cache jbcCache, String regionName, String regionPrefix, CacheDataDescription metadata) {
        super(jbcCache, regionName, regionPrefix, metadata);
        optimistic = (jbcCache.getConfiguration().getNodeLockingScheme() == NodeLockingScheme.OPTIMISTIC);
    }

    /**
     * {@inheritDoc}
     */
    public EntityRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
        if (AccessType.READ_ONLY.equals(accessType)) {
            return optimistic ? new OptimisticReadOnlyAccess(this) : new ReadOnlyAccess(this);
        }
        if (AccessType.TRANSACTIONAL.equals(accessType)) {
            return optimistic ? new OptimisticTransactionalAccess(this) : new TransactionalAccess(this);
        }

        // todo : add support for READ_WRITE ( + NONSTRICT_READ_WRITE ??? )

        throw new CacheException("unsupported access type [" + accessType.getName() + "]");
    }

    @Override
    protected Fqn<String> createRegionFqn(String regionName, String regionPrefix) {
        return getTypeLastRegionFqn(regionName, regionPrefix, TYPE);
    }
    
    public PutFromLoadValidator getPutFromLoadValidator() {
       return new PutFromLoadValidator(transactionManager);
    }

}
