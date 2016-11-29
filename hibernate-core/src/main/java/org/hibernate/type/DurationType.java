/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.Duration;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.spi.descriptor.java.DurationJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.BigIntTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class DurationType
		extends BasicTypeImpl<Duration>
		implements JdbcLiteralFormatter<Duration> {

	/**
	 * Singleton access
	 */
	public static final DurationType INSTANCE = new DurationType();

	protected DurationType() {
		super( DurationJavaDescriptor.INSTANCE, BigIntTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return Duration.class.getSimpleName();
	}

	@Override
	public JdbcLiteralFormatter<Duration> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(Duration value, Dialect dialect) {
		return LongType.INSTANCE.toJdbcLiteral( value.toNanos(), dialect );
	}
}
