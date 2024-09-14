/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ListType;
import org.hibernate.usertype.UserCollectionType;

/**
 * A mapping model object representing a collection of type {@link java.util.List}.
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
	public List(Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver, PersistentClass owner, MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, owner, buildingContext );
	}

	protected List(List original) {
		super( original );
		this.baseIndex = original.baseIndex;
	}

	@Override
	public List copy() {
		return new List( this );
	}

	public boolean isList() {
		return true;
	}

	public CollectionType getDefaultCollectionType() throws MappingException {
		return new ListType( getRole(), getReferencedPropertyName() );
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
