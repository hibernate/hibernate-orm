/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

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
public class CollectionJavaTypeDescriptor<C> extends AbstractClassTypeDescriptor<C> {
	private final CollectionSemantics<C,?> semantics;

	@SuppressWarnings("unchecked")
	public CollectionJavaTypeDescriptor(Class<? extends C> type, CollectionSemantics<C,?> semantics) {
		super( (Class) type );
		this.semantics = semantics;
	}

	public CollectionSemantics<C,?> getSemantics() {
		return semantics;
	}

	@Override
	public JdbcTypeDescriptor getRecommendedJdbcType(JdbcTypeDescriptorIndicators context) {
		// none
		return null;
	}

	@Override
	public C fromString(CharSequence string) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> X unwrap(C value, Class<X> type, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> C wrap(X value, WrapperOptions options) {
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
