/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.collection.internal.StandardListSemantics;
import org.hibernate.collection.spi.CollectionSemantics;

/**
 * A list mapping has a primary key consisting of the key columns + index column.
 *
 * @author Gavin King
 */
public class List extends IndexedCollection {
	private final JavaTypeMapping javaTypeMapping;
	private int baseIndex;

	public boolean isList() {
		return true;
	}

	public List(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );

		javaTypeMapping = new CollectionJavaTypeMapping(
				buildingContext.getBootstrapContext().getTypeConfiguration(),
				java.util.List.class
		);
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

	@Override
	public JavaTypeMapping getJavaTypeMapping() {
		return javaTypeMapping;
	}

	@Override
	@SuppressWarnings("unchecked")
	public CollectionSemantics getCollectionSemantics() {
		return StandardListSemantics.INSTANCE;
	}

}
