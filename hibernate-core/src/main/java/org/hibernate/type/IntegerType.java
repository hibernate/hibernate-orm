/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.VersionSupport;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.IntegerJavaDescriptor;

/**
 * A type that maps between {@link java.sql.Types#INTEGER INTEGER} and @link Integer}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class IntegerType extends BasicTypeImpl<Integer>
		implements VersionSupport<Integer> {

	public static final IntegerType INSTANCE = new IntegerType();

	public static final Integer ZERO = 0;

	public IntegerType() {
		super( IntegerJavaDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.IntegerTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "integer";
	}

	@Override
	public VersionSupport<Integer> getVersionSupport() {
		return this;
	}

	@Override
	public Integer seed(SharedSessionContractImplementor session) {
		return ZERO;
	}

	@Override
	public Integer next(Integer current, SharedSessionContractImplementor session) {
		return current + 1;
	}

	@Override
	public JdbcLiteralFormatter<Integer> getJdbcLiteralFormatter() {
		return org.hibernate.type.spi.descriptor.sql.IntegerTypeDescriptor.INSTANCE.getJdbcLiteralFormatter(
				IntegerJavaDescriptor.INSTANCE
		);
	}
}
