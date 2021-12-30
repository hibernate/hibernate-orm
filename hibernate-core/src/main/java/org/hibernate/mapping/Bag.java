/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.function.Function;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.collection.internal.StandardBagSemantics;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.type.BagType;
import org.hibernate.type.CollectionType;

/**
 * A bag permits duplicates, so it has no primary key
 * 
 * @author Gavin King
 */
public class Bag extends Collection {
	/**
	 * hbm.xml binding
	 */
	public Bag(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	/**
	 * Annotation binding
	 */
	public Bag(SemanticsResolver semanticsResolver, PersistentClass owner, MetadataBuildingContext buildingContext) {
		super( semanticsResolver, owner, buildingContext );
	}

	public CollectionType getDefaultCollectionType() {
		return new BagType( getMetadata().getTypeConfiguration(), getRole(), getReferencedPropertyName() );
	}

	void createPrimaryKey() {
		//create an index on the key columns??
	}

	@Override
	public CollectionSemantics getDefaultCollectionSemantics() {
		return StandardBagSemantics.INSTANCE;
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
