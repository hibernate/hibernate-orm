/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.type.descriptor.java.internal.EnumJavaDescriptor;
import org.hibernate.type.descriptor.spi.SqlTypeDescriptorIndicators;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.TinyIntSqlDescriptor;

/**
 * @author Chris Cranford
 */
public class RevisionTypeJavaDescriptor extends EnumJavaDescriptor<RevisionType> {
	public static final RevisionTypeJavaDescriptor INSTANCE = new RevisionTypeJavaDescriptor();

	private RevisionTypeJavaDescriptor() {
		super( RevisionType.class );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(SqlTypeDescriptorIndicators context) {
		return TinyIntSqlDescriptor.INSTANCE;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(RevisionType value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( RevisionType.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( Integer.class.isAssignableFrom( type ) ) {
			return (X) Integer.valueOf( value.ordinal() );
		}
		if ( Byte.class.isAssignableFrom( type ) ) {
			return (X) value.getRepresentation();
		}
		throw unknownUnwrap( type );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> RevisionType wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof RevisionType ) {
			return (RevisionType) value;
		}
		if ( value instanceof Integer ) {
			int ordinal = (Integer) value;
			return RevisionType.values()[ ordinal ];
		}
		if ( value instanceof Byte ) {
			return RevisionType.fromRepresentation( value );
		}
		throw unknownWrap( value.getClass() );
	}
}
