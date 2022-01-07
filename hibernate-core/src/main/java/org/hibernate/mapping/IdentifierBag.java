/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.function.Supplier;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.collection.internal.StandardIdentifierBagSemantics;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.CollectionType;
import org.hibernate.type.IdentifierBagType;
import org.hibernate.usertype.UserCollectionType;

/**
 * An {@code IdentifierBag} has a primary key consisting of
 * just the identifier column
 */
public class IdentifierBag extends IdentifierCollection {

	/**
	 * hbm.xml binding
	 */
	public IdentifierBag(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	/**
	 * annotation binding
	 */
	public IdentifierBag(Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver, PersistentClass owner, MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, owner, buildingContext );
	}

	public CollectionType getDefaultCollectionType() {
		return new IdentifierBagType( getMetadata().getTypeConfiguration(), getRole(), getReferencedPropertyName() );
	}

	@Override
	public CollectionSemantics getDefaultCollectionSemantics() {
		return StandardIdentifierBagSemantics.INSTANCE;
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}	
}
