/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Comparator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.VersionSupport;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.ShortJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.SmallIntTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#SMALLINT SMALLINT} and {@link Short}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ShortType extends BasicTypeImpl<Short> implements VersionSupport<Short> {

	public static final ShortType INSTANCE = new ShortType();

	private static final Short ZERO = (short) 0;

	public ShortType() {
		super( ShortJavaDescriptor.INSTANCE, SmallIntTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "short";
	}

	@Override
	public VersionSupport<Short> getVersionSupport() {
		return this;
	}

	@Override
	public Short seed(SharedSessionContractImplementor session) {
		return ZERO;
	}

	@Override
	public Short next(Short current, SharedSessionContractImplementor session) {
		return (short) ( current + 1 );
	}

	@Override
	public Comparator<Short> getComparator() {
		return getJavaTypeDescriptor().getComparator();
	}

	public Serializable getDefaultValue() {
		return ZERO;
	}

	@Override
	public JdbcLiteralFormatter<Short> getJdbcLiteralFormatter() {
		return SmallIntTypeDescriptor.INSTANCE.getJdbcLiteralFormatter( ShortJavaDescriptor.INSTANCE );
	}
}
