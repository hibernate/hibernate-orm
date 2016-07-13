/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.util.Comparator;
import javax.persistence.AttributeConverter;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.MutabilityPlan;

/**
 * Redefines the Type contract in terms of "basic" or "value" types.  All Type methods are implemented
 * using delegation with the bundled SqlTypeDescriptor, JavaTypeDescriptor and AttributeConverter.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
public interface BasicType<T> extends Type<T>, org.hibernate.sqm.domain.BasicType<T>, javax.persistence.metamodel.BasicType<T> {
	@Override
	JavaTypeDescriptor<T> getJavaTypeDescriptor();

	@Override
	MutabilityPlan<T> getMutabilityPlan();

	@Override
	Comparator<T> getComparator();

	/**
	 * Describes the column mapping for this BasicType.
	 *
	 * @return The column mapping for this BasicType
	 */
	ColumnMapping getColumnMapping();

	/**
	 * The converter applied to this type, if one.
	 *
	 * @return The applied converter.
	 */
	AttributeConverter<T,?> getAttributeConverter();

	@Override
	default Classification getClassification() {
		return Classification.BASIC;
	}

	@Override
	default String getName() {
		return getTypeName();
	}

	@Override
	default Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaTypeClass();
	}

	@Override
	@SuppressWarnings("unchecked")
	default String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return getJavaTypeDescriptor().extractLoggableRepresentation( (T) value );
	}

	@Override
	default Type getSemiResolvedType(SessionFactoryImplementor factory) {
		return this;
	}

	@Override
	default Object resolve(Object value, SessionImplementor session, Object owner) {
		return value;
	}

	@Override
	default Object semiResolve(Object value, SessionImplementor session, Object owner) {
		return value;
	}
}
