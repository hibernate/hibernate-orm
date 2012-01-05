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

import java.util.HashSet;
import java.util.Set;

import org.gradle.BuildAdapter;
import org.gradle.BuildResult;

import org.hibernate.build.qalab.DatabaseAllocation;

/**
 * A Gradle {@link org.gradle.BuildListener} used to release all databases allocated when the build is finished.
 *
 * @author Steve Ebersole
 */
public class DatabaseAllocationCleanUp extends BuildAdapter {
	private Set<DatabaseAllocation> databaseAllocations = new HashSet<DatabaseAllocation>();

	public void addDatabaseAllocation(DatabaseAllocation databaseAllocation) {
		databaseAllocations.add( databaseAllocation );
	}

	@Override
	public void buildFinished(BuildResult result) {
		super.buildFinished( result );
		for ( DatabaseAllocation databaseAllocation : databaseAllocations ) {
			databaseAllocation.release();
		}
	}
}
