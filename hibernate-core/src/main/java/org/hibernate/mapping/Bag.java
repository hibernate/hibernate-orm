/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.type.CollectionType;

/**
 * A bag permits duplicates, so it has no primary key
 * 
 * @author Gavin King
 */
public class Bag extends Collection {
	public Bag(MetadataImplementor metadata, PersistentClass owner) {
		super( metadata, owner );
	}

	public CollectionType getDefaultCollectionType() {
		return getMetadata().getTypeResolver()
				.getTypeFactory()
				.bag( getRole(), getReferencedPropertyName() );
	}

	void createPrimaryKey() {
		//create an index on the key columns??
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
