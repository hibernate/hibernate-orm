/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;

import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionMetadata;

/**
 * @author Andrea Boriero
 */
public class SetType extends AbstractCollectionType {
	public SetType(String roleName) {
		super( roleName );
	}

	public SetType(String roleName, Comparator comparator) {
		super( roleName, comparator );
	}

	@Override
	public PersistentCollection instantiate(
			SharedSessionContractImplementor session, PersistentCollectionMetadata persister, Serializable key) {
		return new PersistentSet( session );
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0
				? new HashSet()
				: new HashSet( anticipatedSize + (int) ( anticipatedSize * .75f ), .75f );

	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentSet( session, (java.util.Set) collection );
	}

	@Override
	public Class getReturnedClass() {
		return java.util.Set.class;
	}
}
