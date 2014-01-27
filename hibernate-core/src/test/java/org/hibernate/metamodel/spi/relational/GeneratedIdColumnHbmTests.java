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
package org.hibernate.metamodel.spi.relational;

import org.hibernate.metamodel.MetadataSources;

/**
 * Identity column tests of {@code hbm.xml} binding code
 *
 * @author Gail Badner
 */
public class GeneratedIdColumnHbmTests extends AbstractGeneratedIdColumnTests {
	private static String FILENAME = "org/hibernate/metamodel/spi/relational/GeneratedIdColumn.hbm.xml";

	@Override
	public void addSourcesForNativeId(MetadataSources sources) {
		sources.addResource( FILENAME );
	}

	@Override
	public void addSourcesForSequenceId(MetadataSources sources) {
		sources.addResource( FILENAME );
	}

	@Override
	public void addSourcesForIdentityId(MetadataSources sources) {
		sources.addResource( FILENAME );
	}

	@Override
	public void addSourcesForAssignedId(MetadataSources sources) {
		sources.addResource( FILENAME );
	}
}
