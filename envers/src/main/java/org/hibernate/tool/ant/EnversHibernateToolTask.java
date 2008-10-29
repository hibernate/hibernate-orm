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
package org.hibernate.tool.ant;

import org.apache.tools.ant.BuildException;
import org.hibernate.envers.ant.AnnotationConfigurationTaskWithEnvers;
import org.hibernate.envers.ant.ConfigurationTaskWithEnvers;
import org.hibernate.envers.ant.JPAConfigurationTaskWithEnvers;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EnversHibernateToolTask extends HibernateToolTask {
    private void checkConfiguration() {
        if (configurationTask!=null) {
            throw new BuildException("Only a single configuration is allowed.");
        }
    }

    public JPAConfigurationTask createJpaConfiguration() {
        checkConfiguration();
        JPAConfigurationTask task = new JPAConfigurationTaskWithEnvers();
        configurationTask = task;
        return task;
    }

    public ConfigurationTask createConfiguration() {
        checkConfiguration();
        ConfigurationTaskWithEnvers task = new ConfigurationTaskWithEnvers();
        configurationTask = task;
        return task;
    }

    public AnnotationConfigurationTask createAnnotationConfiguration() {
        checkConfiguration();
        AnnotationConfigurationTaskWithEnvers task = new AnnotationConfigurationTaskWithEnvers();
        configurationTask = task;
        return task;
    }
}
