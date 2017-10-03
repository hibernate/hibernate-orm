/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.lang.reflect.Member;

import org.hibernate.property.access.spi.PropertyAccess;

/**
 * Models a persistent (mapped) attribute in Hibernate's "runtime model".
 *
 * @author Steve Ebersole
 */
public interface PersistentAttribute<O, J> extends Navigable<J>, javax.persistence.metamodel.Attribute<O, J> {
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
	default Class<J> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	PropertyAccess getPropertyAccess();

	boolean isIncludedInOptimisticLocking();



	@Override
	default Member getJavaMember() {
		return getPropertyAccess().getGetter().getMember();
	}

	// todo (6.0) : this method should accept the SqlExpressionQualifier/ColumnReferenceSource/TableGroup
	//		e.g.,
	//			SqlSelectionGroup resolveSqlSelectionGroup(
	//					SqlExpressionQualifier qualifier,
	// 					SqlSelectionGroupResolutionContext resolutionContext
	// 			);
	//
}
