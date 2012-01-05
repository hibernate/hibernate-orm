/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.build.gradle.testing.database;

import java.io.File;
import java.util.Map;

import org.gradle.api.artifacts.Configuration;

import org.hibernate.build.qalab.DatabaseAllocation;

/**
 * Contract for database "profiles".
 *
 * @author Steve Ebersole
 * @author Strong Liu
 */
public interface DatabaseProfile {
	public String getName();
	public File getDirectory();
	public Map<String,String> getHibernateProperties();
	public Configuration getTestingRuntimeConfiguration();
	public DatabaseAllocation getDatabaseAllocation();
}
