/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.function.Function;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.collection.internal.StandardListSemantics;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ListType;

/**
 * A list mapping has a primary key consisting of the key columns + index column.
 *
 * @author Gavin King
 */
public class List extends IndexedCollection {
	
	private int baseIndex;

	/**
	 * hbm.xml binding
	 */
	public List(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	/**
	 * annotation binding
	 */
	public List(SemanticsResolver semanticsResolver, PersistentClass owner, MetadataBuildingContext buildingContext) {
		super( semanticsResolver, owner, buildingContext );
	}

	public boolean isList() {
		return true;
	}

	@Override
	public CollectionSemantics getDefaultCollectionSemantics() {
		return StandardListSemantics.INSTANCE;
	}

	public CollectionType getDefaultCollectionType() throws MappingException {
		return new ListType( getMetadata().getTypeConfiguration(), getRole(), getReferencedPropertyName() );
	}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public int getBaseIndex() {
		return baseIndex;
	}
	
	public void setBaseIndex(int baseIndex) {
		this.baseIndex = baseIndex;
	}
}
