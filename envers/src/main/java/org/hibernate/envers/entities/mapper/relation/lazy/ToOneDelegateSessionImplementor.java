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
 */
package org.hibernate.envers.entities.mapper.relation.lazy;

import java.io.Serializable;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.EntitiesConfigurations;
import org.hibernate.envers.reader.AuditReaderImplementor;

import org.hibernate.HibernateException;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Tomasz Bech
 * @author Hern�n Chanfreau
 */
public class ToOneDelegateSessionImplementor extends AbstractDelegateSessionImplementor {
	private static final long serialVersionUID = 4770438372940785488L;
	
    private final AuditReaderImplementor versionsReader;
    private final Class<?> entityClass;
    private final Object entityId;
    private final Number revision;
    private EntitiesConfigurations entCfg;

	public ToOneDelegateSessionImplementor(AuditReaderImplementor versionsReader,
                                           Class<?> entityClass, Object entityId, Number revision,
                                           AuditConfiguration verCfg) {
        super(versionsReader.getSessionImplementor());
        this.versionsReader = versionsReader;
        this.entityClass = entityClass;
        this.entityId = entityId;
        this.revision = revision;
        this.entCfg = verCfg.getEntCfg();
    }

    public Object doImmediateLoad(String entityName) throws HibernateException {
    	if(entCfg.getNotVersionEntityConfiguration(entityName) == null){
    		// audited relation, look up entity with envers
			return versionsReader.find(entityClass, entityName, entityId, revision);
		} else {
			// notAudited relation, look up entity with hibernate
			return delegate.immediateLoad(entityName, (Serializable) entityId);
		}
    }
}
