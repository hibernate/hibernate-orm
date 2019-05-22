/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;

/**
 * Extension of the general JavaTypeDescriptor for "collection types"
 *
 * @apiNote "Collection types" are defined loosely here to cover mapping
 * collection types other than those from the "Java Collection Framework".
 *
 * @see CollectionSemantics
 *
 * @author Steve Ebersole
 */
public class CollectionJavaTypeDescriptor<C> extends AbstractTypeDescriptor<C> {
	private final CollectionSemantics<C> semantics;

	@SuppressWarnings("unchecked")
	public CollectionJavaTypeDescriptor(Class<? extends C> type, CollectionSemantics<C> semantics) {
		super( (Class) type );
		this.semantics = semantics;
	}

	public CollectionSemantics<C> getSemantics() {
		return semantics;
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(SqlTypeDescriptorIndicators context) {
		// none
		return null;
	}

	@Override
	public C fromString(String string) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> X unwrap(C value, Class<X> type, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> C wrap(X value, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean areEqual(C one, C another) {
		return one == another ||
				(
						one instanceof PersistentCollection &&
								( (PersistentCollection) one ).wasInitialized() &&
								( (PersistentCollection) one ).isWrapper( another )
				) ||
				(
						another instanceof PersistentCollection &&
								( (PersistentCollection) another ).wasInitialized() &&
								( (PersistentCollection) another ).isWrapper( one )
				);
	}

	@Override
	public int extractHashCode(C x) {
		throw new UnsupportedOperationException();
	}
}
