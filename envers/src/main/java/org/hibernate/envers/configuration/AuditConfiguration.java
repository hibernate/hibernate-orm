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
package org.hibernate.envers.configuration;

import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import org.hibernate.envers.entities.EntitiesConfigurations;
import org.hibernate.envers.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.synchronization.AuditProcessManager;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.annotations.common.reflection.ReflectionManager;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditConfiguration {
    private final GlobalConfiguration globalCfg;
    private final AuditEntitiesConfiguration auditEntCfg;
    private final AuditProcessManager auditProcessManager;
    private final EntitiesConfigurations entCfg;
    private final RevisionInfoQueryCreator revisionInfoQueryCreator;
    private final RevisionInfoNumberReader revisionInfoNumberReader;

    public AuditEntitiesConfiguration getAuditEntCfg() {
        return auditEntCfg;
    }

    public AuditProcessManager getSyncManager() {
        return auditProcessManager;
    }

    public GlobalConfiguration getGlobalCfg() {
        return globalCfg;
    }

    public EntitiesConfigurations getEntCfg() {
        return entCfg;
    }

    public RevisionInfoQueryCreator getRevisionInfoQueryCreator() {
        return revisionInfoQueryCreator;
    }

    public RevisionInfoNumberReader getRevisionInfoNumberReader() {
        return revisionInfoNumberReader;
    }

    @SuppressWarnings({"unchecked"})
    public AuditConfiguration(Configuration cfg) {
        Properties properties = cfg.getProperties();

        ReflectionManager reflectionManager = ((AnnotationConfiguration) cfg).getReflectionManager();
        RevisionInfoConfiguration revInfoCfg = new RevisionInfoConfiguration();
        RevisionInfoConfigurationResult revInfoCfgResult = revInfoCfg.configure(cfg, reflectionManager);
        auditEntCfg = new AuditEntitiesConfiguration(properties, revInfoCfgResult.getRevisionInfoEntityName());
        globalCfg = new GlobalConfiguration(properties);
        auditProcessManager = new AuditProcessManager(revInfoCfgResult.getRevisionInfoGenerator());
        revisionInfoQueryCreator = revInfoCfgResult.getRevisionInfoQueryCreator();
        revisionInfoNumberReader = revInfoCfgResult.getRevisionInfoNumberReader();
        entCfg = new EntitiesConfigurator().configure(cfg, reflectionManager, globalCfg, auditEntCfg,
                revInfoCfgResult.getRevisionInfoXmlMapping(), revInfoCfgResult.getRevisionInfoRelationMapping());
    }

    //

    private static Map<Configuration, AuditConfiguration> cfgs
            = new WeakHashMap<Configuration, AuditConfiguration>();

    public synchronized static AuditConfiguration getFor(Configuration cfg) {
        AuditConfiguration verCfg = cfgs.get(cfg);

        if (verCfg == null) {
            verCfg = new AuditConfiguration(cfg);
            cfgs.put(cfg, verCfg);
            
            cfg.buildMappings();
        }

        return verCfg;
    }
}
