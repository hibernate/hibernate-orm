/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.event.collection.association.unidirectional.onetomany;

import java.util.Collection;
import java.util.HashSet;

import org.hibernate.test.event.collection.ParentWithCollection;
import org.hibernate.test.event.collection.association.AbstractAssociationCollectionEventTest;
import org.hibernate.test.event.collection.association.unidirectional.ParentWithCollectionOfEntities;

/**
 * @author Gail Badner
 */
public class UnidirectionalOneToManySetCollectionEventTest extends AbstractAssociationCollectionEventTest {
	@Override
	public String[] getMappings() {
		return new String[] { "event/collection/association/unidirectional/onetomany/UnidirectionalOneToManySetMapping.hbm.xml" };
	}

	@Override
	public ParentWithCollection createParent(String name) {
		return new ParentWithCollectionOfEntities( name );
	}

	@Override
	public Collection createCollection() {
		return new HashSet();
	}	
}

