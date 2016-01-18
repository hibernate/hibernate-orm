/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.sql.TimestampTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class OffsetDateTimeType
		extends AbstractSingleColumnStandardBasicType<OffsetDateTime>
		implements VersionType<OffsetDateTime>, LiteralType<OffsetDateTime> {

	/**
	 * Singleton access
	 */
	public static final OffsetDateTimeType INSTANCE = new OffsetDateTimeType();

	public OffsetDateTimeType() {
		super( VarcharTypeDescriptor.INSTANCE, OffsetDateTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public String objectToSQLString(OffsetDateTime value, Dialect dialect) throws Exception {
		return value.toString();
	}

	@Override
	public OffsetDateTime seed(SessionImplementor session) {
		return OffsetDateTime.now();
	}

	@Override
	public OffsetDateTime next(OffsetDateTime current, SessionImplementor session) {
		return OffsetDateTime.now();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Comparator<OffsetDateTime> getComparator() {
		return ComparableComparator.INSTANCE;
	}

	@Override
	public String getName() {
		return OffsetDateTime.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
