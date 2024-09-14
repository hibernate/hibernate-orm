/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
