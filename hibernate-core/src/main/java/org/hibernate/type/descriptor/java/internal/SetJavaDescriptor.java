/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.Set;

import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SetJavaDescriptor extends AbstractBasicJavaDescriptor<Set> {
	/**
	 * Singleton access
	 */
	public static final SetJavaDescriptor INSTANCE = new SetJavaDescriptor();

	private SetJavaDescriptor() {
		super( Set.class );
	}

	@Override
	public Class<Set> getJavaType() {
		return Set.class;
	}

	@Override
	public String getTypeName() {
		return getJavaType().getName();
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		// none
		return null;
	}

	@Override
	public int extractHashCode(Set value) {
		return value.hashCode();
	}

	@Override
	public boolean areEqual(Set one, Set another) {
		return one == another
				|| ( one != null && one.equals( another ) );
	}

	@Override
	public String extractLoggableRepresentation(Set value) {
		return "{list}";
	}

	@Override
	public String toString(Set value) {
		return "{list}";
	}

	@Override
	public Set fromString(String string) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> X unwrap(Set value, Class<X> type, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> Set wrap(X value, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}
}
