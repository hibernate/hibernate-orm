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
package org.hibernate.metamodel.spi.binding;

import static org.junit.Assert.assertEquals;

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.internal.MetadataImpl;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;
import org.junit.Test;

/**
 * "Unsaved" value tests of {@code hbm.xml} binding code
 *
 * @author Gail Badner
 */
public class UnsavedValueHbmTests extends AbstractUnsavedValueTests {

	@Test
	@FailureExpectedWithNewUnifiedXsd(message = "unsaved-value")
	public void testAssignedSimpleIdNonDefaultUnsavedValue() {
		MetadataSources sources = new MetadataSources( basicServiceRegistry() );
		addNonDefaultSources( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( EntityWithAssignedId.class.getName() );
		// unsaved-value was mapped as "any"; that should be used, regardless of ID generator.
		assertEquals( "any", entityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getUnsavedValue() );
	}

	@Test
	@FailureExpectedWithNewUnifiedXsd(message = "unsaved-value")
	public void testIncrementSimpleIdNonDefaultUnsavedValue() {
		MetadataSources sources = new MetadataSources( basicServiceRegistry() );
		addNonDefaultSources( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( EntityWithSequenceId.class.getName() );
		// unsaved-value was mapped as "null"; that should be used, regardless of ID generator.
		assertEquals( "null", entityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getUnsavedValue() );
	}

	@Test
	@FailureExpectedWithNewUnifiedXsd(message = "unsaved-value")
	public void testNonDefaultUnsavedVersion() {
		MetadataSources sources = new MetadataSources( basicServiceRegistry() );
		addNonDefaultSources( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( EntityWithVersion.class.getName() );
		// version unsaved-value was mapped as "negative".
		assertEquals( "negative", entityBinding.getHierarchyDetails().getEntityVersion().getUnsavedValue() );
	}

	@Test
	@FailureExpectedWithNewUnifiedXsd(message = "unsaved-value")
	public void testNonDefaultUnsavedTimestamp() {
		MetadataSources sources = new MetadataSources( basicServiceRegistry() );
		addNonDefaultSources( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( EntityWithTimestamp.class.getName() );
		// version unsaved-value was mapped as "null".
		assertEquals( "null", entityBinding.getHierarchyDetails().getEntityVersion().getUnsavedValue() );
	}


	public void addSourcesForDefaultSimpleIdDefaultUnsavedValue(MetadataSources sources) {
		addDefaultSources( sources );
	}

	public void addSourcesForAssignedIdDefaultUnsavedValue(MetadataSources sources) {
		addDefaultSources( sources );
	}

	public void addSourcesForSequenceIdDefaultUnsavedValue(MetadataSources sources) {
		addDefaultSources( sources );
	}

	public void addSourcesForDefaultUnsavedVersion(MetadataSources sources) {
		addDefaultSources( sources );
	}

	public void addSourcesForDefaultUnsavedTimestamp(MetadataSources sources) {
		addDefaultSources( sources );
	}

	private void addDefaultSources(MetadataSources sources) {
		sources.addResource( "org/hibernate/metamodel/spi/binding/UnsavedDefaultValues.hbm.xml" );
	}

	private void addNonDefaultSources(MetadataSources sources) {
		sources.addResource( "org/hibernate/metamodel/spi/binding/UnsavedNonDefaultValues.hbm.xml" );
	}
}
