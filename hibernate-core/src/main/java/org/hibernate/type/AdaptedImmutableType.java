/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.spi.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.spi.descriptor.java.MutabilityPlan;

/**
 * Optimize a mutable type, if the user promises not to mutable the
 * instances.
 * 
 * @author Gavin King
 * @author Steve Ebersole
 */
public class AdaptedImmutableType<T> extends BasicTypeImpl<T> {
	private final String name;
	private final JdbcLiteralFormatter<T> jdbcLiteralFormatter;

	public AdaptedImmutableType(BasicType<T> baseMutableType) {
		super(
				baseMutableType.getJavaTypeDescriptor(),
				baseMutableType.getColumnMapping().getSqlTypeDescriptor(),
				ImmutableMutabilityPlan.INSTANCE,
				baseMutableType.getComparator()
		);
		this.name = "imm_" + baseMutableType.getName();
		this.jdbcLiteralFormatter = baseMutableType.getJdbcLiteralFormatter();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public MutabilityPlan<T> getMutabilityPlan() {
		return ImmutableMutabilityPlan.INSTANCE;
	}

	public String getName() {
		return name;
	}

	@Override
	public JdbcLiteralFormatter<T> getJdbcLiteralFormatter() {
		return jdbcLiteralFormatter;
	}
}
