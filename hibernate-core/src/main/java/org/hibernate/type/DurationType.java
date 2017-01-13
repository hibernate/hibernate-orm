/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.Duration;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.DurationJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.BigIntSqlDescriptor;

/**
 * @author Steve Ebersole
 */
public class DurationType
		extends BasicTypeImpl<Duration> {

	/**
	 * Singleton access
	 */
	public static final DurationType INSTANCE = new DurationType();

	protected DurationType() {
		super( DurationJavaDescriptor.INSTANCE, BigIntSqlDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return Duration.class.getSimpleName();
	}

	@Override
	public JdbcLiteralFormatter<Duration> getJdbcLiteralFormatter() {
		return BigIntSqlDescriptor.INSTANCE.getJdbcLiteralFormatter( DurationJavaDescriptor.INSTANCE );
	}
}
