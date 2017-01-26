/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.envers.RevisionType;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Chris Cranford
 */
public class RevisionTypeJavaDescriptor extends AbstractBasicJavaDescriptor<RevisionType> {
	public static final RevisionTypeJavaDescriptor INSTANCE = new RevisionTypeJavaDescriptor();

	private RevisionTypeJavaDescriptor() {
		super( RevisionType.class );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return null;
	}

	@Override
	public String toString(RevisionType value) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public RevisionType fromString(String string) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public <X> X unwrap(RevisionType value, Class<X> type, WrapperOptions options) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public <X> RevisionType wrap(X value, WrapperOptions options) {
		throw new NotYetImplementedException(  );
	}
}
