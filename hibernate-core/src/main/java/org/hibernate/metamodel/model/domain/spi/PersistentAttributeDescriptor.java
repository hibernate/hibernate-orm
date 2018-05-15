/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.lang.reflect.Member;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.property.access.spi.PropertyAccess;

/**
 * Models a persistent (mapped) attribute in Hibernate's "runtime model".
 *
 * @author Steve Ebersole
 */
public interface PersistentAttributeDescriptor<O, J> extends Navigable<J>, PersistentAttribute<O, J> {
	/**
	 * Get the attribute's position within the ManagedType hierarchy.  The
	 * position follows a pre-defined algorithm based on alphabetical order,
	 * super types first.  The idea for the pre-defined algorithm is that
	 * external sources (bytecode enhancement, e.g.) can determine this
	 * order without having to actually build the ManagedTypes.
	 *
	 * @apiNote Intended for use by byte-code enhancement in order to
	 * be able to use simple arrays in its enhancement of the managed-types
	 * and be able to communicate with (to/from) Hibernate (Session typically)
	 * in terms of just this simple int value, as opposed to it having to
	 * resolve this to a PersistentAttribute and pass that back in - avoiding
	 * multiple dispatches.
	 */
	default int getAttributePosition() {
		throw new NotYetImplementedFor6Exception();
	}

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

	SimpleTypeDescriptor<?> getValueGraphType();
	SimpleTypeDescriptor<?> getKeyGraphType();

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
