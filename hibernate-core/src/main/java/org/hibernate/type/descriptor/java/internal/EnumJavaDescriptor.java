/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.sql.Types;
import javax.persistence.EnumType;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.spi.SqlTypeDescriptorIndicators;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Describes a Java Enum type.
 *
 * @author Steve Ebersole
 */
public class EnumJavaDescriptor<E extends Enum> extends AbstractBasicJavaDescriptor<E>
		implements org.hibernate.type.descriptor.java.spi.EnumJavaDescriptor<E> {

	@SuppressWarnings("unchecked")
	public EnumJavaDescriptor(Class<E> type) {
		super( type, ImmutableMutabilityPlan.INSTANCE );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(SqlTypeDescriptorIndicators context) {
		if ( context.getEnumeratedType() != null && context.getEnumeratedType() == EnumType.STRING ) {
			return context.isNationalized()
					? context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.NVARCHAR )
					: context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.VARCHAR );
		}
		else {
			return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.INTEGER );
		}
	}

	@Override
	public String toString(E value) {
		return value == null ? "<null>" : value.name();
	}

	@Override
	@SuppressWarnings({"unchecked", "OptionalGetWithoutIsPresent"})
	public E fromString(String string) {
		return string == null ? null : (E) Enum.valueOf( getJavaType(), string );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(E value, Class<X> type, SharedSessionContractImplementor session) {
		return (X) value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> E wrap(X value, SharedSessionContractImplementor session) {
		return (E) value;
	}

	@Override
	public Integer toOrdinal(E domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		return domainForm.ordinal();
	}

	@Override
	@SuppressWarnings("unchecked")
	public E fromOrdinal(Integer relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		return (E) getJavaType().getEnumConstants()[ relationalForm ];
	}

	@Override
	@SuppressWarnings("unchecked")
	public E fromName(String relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		return (E) Enum.valueOf( getJavaType(), relationalForm );
	}

	@Override
	public String toName(E domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		return domainForm.name();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getJavaType().getName() + ")";
	}
}
