/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.collection.internal.StandardBagSemantics;
import org.hibernate.collection.spi.CollectionSemantics;

/**
 * A bag permits duplicates, so it has no primary key
 * 
 * @author Gavin King
 */
public class Bag extends Collection {
	private final CollectionJavaTypeMapping javaTypeMapping;

	public Bag(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );

		javaTypeMapping = new CollectionJavaTypeMapping(
				buildingContext.getBootstrapContext().getTypeConfiguration(),
				java.util.Collection.class
		);
	}

	void createPrimaryKey() {
		//create an index on the key columns??
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Override
	@SuppressWarnings("unchecked")
	public CollectionSemantics getCollectionSemantics() {
		return StandardBagSemantics.INSTANCE;
	}

	@Override
	public JavaTypeMapping getJavaTypeMapping() {
		return javaTypeMapping;
	}
}
