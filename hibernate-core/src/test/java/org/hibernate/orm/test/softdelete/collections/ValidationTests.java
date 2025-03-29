/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.collections;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.metamodel.UnsupportedMappingException;

import org.junit.jupiter.api.Test;


/**
 * @author Steve Ebersole
 */
public class ValidationTests {
	@Test
	void testOneToMany() {
		try {
			final Metadata metadata = new MetadataSources()
					.addAnnotatedClass( InvalidCollectionOwner.class )
					.addAnnotatedClass( CollectionOwned.class )
					.buildMetadata();
		}
		catch (UnsupportedMappingException expected) {
		}
	}
}
