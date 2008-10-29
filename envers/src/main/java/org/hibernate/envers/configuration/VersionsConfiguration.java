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
package org.jboss.envers.configuration;

import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import org.jboss.envers.entities.EntitiesConfigurations;
import org.jboss.envers.revisioninfo.RevisionInfoNumberReader;
import org.jboss.envers.revisioninfo.RevisionInfoQueryCreator;
import org.jboss.envers.synchronization.VersionsSyncManager;
import org.jboss.envers.tools.reflection.YReflectionManager;

import org.hibernate.cfg.Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class VersionsConfiguration {
    private final GlobalConfiguration globalCfg;
    private final VersionsEntitiesConfiguration verEntCfg;
    private final VersionsSyncManager versionsSyncManager;
    private final EntitiesConfigurations entCfg;
    private final RevisionInfoQueryCreator revisionInfoQueryCreator;
    private final RevisionInfoNumberReader revisionInfoNumberReader;

    public VersionsEntitiesConfiguration getVerEntCfg() {
        return verEntCfg;
    }

    public VersionsSyncManager getSyncManager() {
        return versionsSyncManager;
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
    public VersionsConfiguration(Configuration cfg) {
        Properties properties = cfg.getProperties();

        YReflectionManager reflectionManager = YReflectionManager.get(cfg);
        RevisionInfoConfiguration revInfoCfg = new RevisionInfoConfiguration();
        RevisionInfoConfigurationResult revInfoCfgResult = revInfoCfg.configure(cfg, reflectionManager);
        verEntCfg = new VersionsEntitiesConfiguration(properties, revInfoCfgResult.getRevisionInfoEntityName());
        globalCfg = new GlobalConfiguration(properties);
        versionsSyncManager = new VersionsSyncManager(revInfoCfgResult.getRevisionInfoGenerator());
        revisionInfoQueryCreator = revInfoCfgResult.getRevisionInfoQueryCreator();
        revisionInfoNumberReader = revInfoCfgResult.getRevisionInfoNumberReader();
        entCfg = new EntitiesConfigurator().configure(cfg, reflectionManager, globalCfg, verEntCfg,
                revInfoCfgResult.getRevisionInfoXmlMapping(), revInfoCfgResult.getRevisionInfoRelationMapping());
    }

    //

    private static Map<Configuration, VersionsConfiguration> cfgs
            = new WeakHashMap<Configuration, VersionsConfiguration>();

    public synchronized static VersionsConfiguration getFor(Configuration cfg) {
        VersionsConfiguration verCfg = cfgs.get(cfg);

        if (verCfg == null) {
            verCfg = new VersionsConfiguration(cfg);
            cfgs.put(cfg, verCfg);
            
            cfg.buildMappings();
        }

        return verCfg;
    }
}
