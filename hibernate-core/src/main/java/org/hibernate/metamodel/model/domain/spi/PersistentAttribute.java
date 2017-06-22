/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.exec.results.spi.SqlSelectionGroup;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;

/**
 * Models a persistent (mapped) attribute in Hibernate's "runtime model".
 *
 * @author Steve Ebersole
 */
public interface PersistentAttribute<O,T> extends Navigable<T>, javax.persistence.metamodel.Attribute<O,T> {
	@Override
	ManagedTypeDescriptor<O> getContainer();

	default String getAttributeName() {
		return getNavigableName();
	}

	@Override
	default String getName() {
		return getNavigableRole().getNavigableName();
	}

	@Override
	default ManagedTypeDescriptor<O> getDeclaringType() {
		return getContainer();
	}

	@Override
	@SuppressWarnings("unchecked")
	default Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	PropertyAccess getPropertyAccess();

	SqlSelectionGroup resolveSqlSelectionGroup(QueryResultCreationContext resolutionContext);
}
