/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain.internal;

import org.hibernate.boot.model.domain.IdentifiableJavaTypeMapping;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * @author Chris Cranford
 */
public abstract class AbstractIdentifiableJavaTypeMapping<T> extends AbstractManagedJavaTypeMapping<T> implements IdentifiableJavaTypeMapping<T> {
	public AbstractIdentifiableJavaTypeMapping(MetadataBuildingContext buildingContext, IdentifiableJavaTypeMapping<? super T> superJavaTypeMapping) {
		super( buildingContext, superJavaTypeMapping );
	}

	@Override
	@SuppressWarnings("unchecked")
	public IdentifiableJavaTypeMapping<? super T> getSuperType() {
		return (IdentifiableJavaTypeMapping<? super T>) super.getSuperType();
	}
}
