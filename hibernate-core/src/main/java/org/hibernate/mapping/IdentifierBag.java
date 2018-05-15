/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.collection.internal.StandardArraySemantics;
import org.hibernate.collection.spi.CollectionSemantics;

/**
 * An <tt>IdentifierBag</tt> has a primary key consisting of
 * just the identifier column
 */
public class IdentifierBag extends IdentifierCollection {
	private final CollectionJavaTypeMapping javaTypeMapping;

	public IdentifierBag(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );

		javaTypeMapping = new CollectionJavaTypeMapping(
				buildingContext.getBootstrapContext().getTypeConfiguration(),
				java.util.Collection.class
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public CollectionSemantics getCollectionSemantics() {
		return StandardArraySemantics.INSTANCE;
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Override
	public JavaTypeMapping getJavaTypeMapping() {
		return javaTypeMapping;
	}
}
