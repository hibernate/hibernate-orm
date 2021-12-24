/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Timestamp;
import java.util.Date;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.descriptor.java.DbTimestampJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.TemporalJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;

import org.jboss.logging.Logger;

/**
 * {@code dbtimestamp}: A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link Timestamp}.
 * It maps to the database's current timestamp, rather than the jvm's
 * current timestamp.
 * <p/>
 * Note: May/may-not cause issues on dialects which do not properly support
 * a true notion of timestamp (Oracle < 8, for example, where only its DATE
 * datatype is supported).  Depends on the frequency of DML operations...
 *
 * @author Steve Ebersole
 */
public class DbTimestampType extends AbstractSingleColumnStandardBasicType<Date> {

	public static final DbTimestampType INSTANCE = new DbTimestampType();

	private DbTimestampType() {
		super( TimestampJdbcType.INSTANCE, new DbTimestampJavaTypeDescriptor<>( JdbcTimestampJavaTypeDescriptor.INSTANCE ) );
	}

	@Override
	public String getName() {
		return "dbtimestamp";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] {getName()};
	}

}
