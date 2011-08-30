/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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

package org.hibernate.gradle.testing.matrix;

import java.io.File;
import java.util.Map;

import hudson.util.DBAllocation;
import org.gradle.api.artifacts.Configuration;

import org.hibernate.gradle.testing.database.DependencyResolver;
import org.hibernate.gradle.util.Jdk;

/**
 * Describes the various pieces of information being contributed to the matrix by a given node.
 *
 * @author Steve Ebersole
 * @author Strong Liu
 */
public interface MatrixNode {
    /**
     * Get the name of this node.
     *
     * @return The node.
     */
    String getName();

    Configuration getTestingRuntimeConfiguration();

    Jdk getTestingRuntimeJdk();

    DependencyResolver getDependencyResolver();
    File getBaseOutputDirectory();
    DBAllocation getDBAllocation();
    Map<String,String> getProperties();
    File getHibernatePropertyFile();
    void setHibernatePropertyFile(File file);
    void release();
}
