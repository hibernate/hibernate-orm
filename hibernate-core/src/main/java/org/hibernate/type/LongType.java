/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Comparator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.VersionSupport;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.LongJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.BigIntSqlDescriptor;

/**
 * A type that maps between {@link java.sql.Types#BIGINT BIGINT} and {@link Long}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class LongType
		extends BasicTypeImpl<Long>
		implements VersionSupport<Long> {

	public static final LongType INSTANCE = new LongType();

	private static final Long ZERO = (long) 0;

	public LongType() {
		super( LongJavaDescriptor.INSTANCE, BigIntSqlDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "long";
	}

	@Override
	public VersionSupport<Long> getVersionSupport() {
		return this;
	}

	@Override
	public Long next(Long current, SharedSessionContractImplementor session) {
		return current + 1L;
	}

	@Override
	public Long seed(SharedSessionContractImplementor session) {
		return ZERO;
	}

	@Override
	public Comparator<Long> getComparator() {
		return getJavaTypeDescriptor().getComparator();
	}

	@Override
	public JdbcLiteralFormatter<Long> getJdbcLiteralFormatter() {
		return BigIntSqlDescriptor.INSTANCE.getJdbcLiteralFormatter( LongJavaDescriptor.INSTANCE );
	}
}
