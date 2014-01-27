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

import org.junit.Test;

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.testing.FailureExpected;

/**
 * Identity column tests of annotations binding code
 *
 * @author Gail Badner
 */
public class GeneratedIdColumnAnnotationsTests extends AbstractGeneratedIdColumnTests {

	@Test
	@FailureExpected( jiraKey = "HHH-7400" )
	public void testNativeId() {
		super.testNativeId();
	}

	@Override
	public void addSourcesForNativeId(MetadataSources sources) {
		sources.addAnnotatedClass( EntityWithAssignedId.class );
	}

	@Override
	public void addSourcesForSequenceId(MetadataSources sources) {
		sources.addAnnotatedClass( EntityWithSequenceId.class );
	}

	@Override
	public void addSourcesForIdentityId(MetadataSources sources) {
		sources.addAnnotatedClass( EntityWithIdentityId.class );
	}

	@Override
	public void addSourcesForAssignedId(MetadataSources sources) {
		sources.addAnnotatedClass( EntityWithAssignedId.class );
	}
}
