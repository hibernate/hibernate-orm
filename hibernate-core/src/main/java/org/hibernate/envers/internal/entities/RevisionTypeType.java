/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.metamodel.model.convert.internal.OrdinalEnumValueConverter;
import org.hibernate.type.descriptor.sql.spi.IntegerSqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;

/**
 * A hibernate type for the {@link RevisionType} enum.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class RevisionTypeType extends BasicTypeImpl<RevisionType> {
	public static final RevisionTypeType INSTANCE = new RevisionTypeType();

	private OrdinalEnumValueConverter converter;

	public RevisionTypeType() {
		super( RevisionTypeJavaDescriptor.INSTANCE, IntegerSqlDescriptor.INSTANCE );
	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
		if ( converter == null ) {
			converter = new OrdinalEnumValueConverter(
					RevisionTypeJavaDescriptor.INSTANCE,
					session.getFactory().getTypeConfiguration()
			);
		}

		return converter.toRelationalValue( (RevisionType) value, session );
	}
}
