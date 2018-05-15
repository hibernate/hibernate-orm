/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.Comparator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Andrea Boriero
 */
public class AnyTypeJavaDescriptor implements JavaTypeDescriptor {
	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return null;
	}

	@Override
	public Object wrap(Object value, SharedSessionContractImplementor session) {
		return null;
	}

	@Override
	public Object unwrap(Object value, Class type, SharedSessionContractImplementor session) {
		return null;
	}

	@Override
	public Class getJavaType() {
		return null;
	}

	@Override
	public String getTypeName() {
		return null;
	}

	@Override
	public MutabilityPlan getMutabilityPlan() {
		return null;
	}

	@Override
	public Comparator getComparator() {
		return null;
	}

	@Override
	public int extractHashCode(Object value) {
		return 0;
	}

	@Override
	public boolean areEqual(Object one, Object another) {
		return false;
	}

	@Override
	public String extractLoggableRepresentation(Object value) {
		return null;
	}
}
