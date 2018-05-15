/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain.internal;

import org.hibernate.boot.model.domain.ManagedJavaTypeMapping;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * @author Chris Cranford
 */
public abstract class AbstractManagedJavaTypeMapping<T> implements ManagedJavaTypeMapping<T> {
	private final MetadataBuildingContext buildingContext;
	private final ManagedJavaTypeMapping<? super T> superTypeMapping;

	public AbstractManagedJavaTypeMapping(MetadataBuildingContext buildingContext, ManagedJavaTypeMapping<? super T> superJavaTypeMapping) {
		this.buildingContext = buildingContext;
		this.superTypeMapping = superJavaTypeMapping;
	}

	@Override
	public ManagedJavaTypeMapping<? super T> getSuperType() {
		return superTypeMapping;
	}

	protected MetadataBuildingContext getMetadataBuildingContext() {
		return buildingContext;
	}
}
