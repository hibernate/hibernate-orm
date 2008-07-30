/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.util.ComparableComparator;

/**
 * <tt>timestamp</tt>: A type that maps an SQL TIMESTAMP to a Java
 * java.util.Date or java.sql.Timestamp.
 * @author Gavin King
 */
public class TimestampType extends MutableType implements VersionType, LiteralType {

	private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public Object get(ResultSet rs, String name) throws SQLException {
		return rs.getTimestamp(name);
	}
	
	public Class getReturnedClass() {
		return java.util.Date.class;
	}
	
	public void set(PreparedStatement st, Object value, int index) throws SQLException {
		Timestamp ts;
		if (value instanceof Timestamp) {
			ts = (Timestamp) value;
		}
		else {
			ts = new Timestamp( ( (java.util.Date) value ).getTime() );
		}
		st.setTimestamp(index, ts);
	}

	public int sqlType() {
		return Types.TIMESTAMP;
	}
	
	public String getName() { return "timestamp"; }

	public String toString(Object val) {
		return new SimpleDateFormat(TIMESTAMP_FORMAT).format( (java.util.Date) val );
	}

	public Object deepCopyNotNull(Object value) {
		if ( value instanceof Timestamp ) {
			Timestamp orig = (Timestamp) value;
			Timestamp ts = new Timestamp( orig.getTime() );
			ts.setNanos( orig.getNanos() );
			return ts;
		}
		else {
			java.util.Date orig = (java.util.Date) value;
			return new java.util.Date( orig.getTime() );
		}
	}

	public boolean isEqual(Object x, Object y) {

		if (x==y) return true;
		if (x==null || y==null) return false;

		long xTime = ( (java.util.Date) x ).getTime();
		long yTime = ( (java.util.Date) y ).getTime();
		boolean xts = x instanceof Timestamp;
		boolean yts = y instanceof Timestamp;
		int xNanos = xts ? ( (Timestamp) x ).getNanos() : 0;
		int yNanos = yts ? ( (Timestamp) y ).getNanos() : 0;
		if ( !Environment.jvmHasJDK14Timestamp() ) {
			xTime += xNanos / 1000000;
			yTime += yNanos / 1000000;
		}
		if ( xTime!=yTime ) return false;
		if (xts && yts) {
			// both are Timestamps
			int xn = xNanos % 1000000;
			int yn = yNanos % 1000000;
			return xn==yn;
		}
		else {
			// at least one is a plain old Date
			return true;
		}

	}

	public int getHashCode(Object x, EntityMode entityMode) {
		java.util.Date ts = (java.util.Date) x;
		return new Long( ts.getTime() / 1000 ).hashCode();
	}

	public Object next(Object current, SessionImplementor session) {
		return seed( session );
	}

	public Object seed(SessionImplementor session) {
		return new Timestamp( System.currentTimeMillis() );
	}

	public Comparator getComparator() {
		return ComparableComparator.INSTANCE;
	}

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return '\'' + new Timestamp( ( (java.util.Date) value ).getTime() ).toString() + '\'';
	}

	public Object fromStringValue(String xml) throws HibernateException {
		try {
			return new Timestamp( new SimpleDateFormat(TIMESTAMP_FORMAT).parse(xml).getTime() );
		}
		catch (ParseException pe) {
			throw new HibernateException("could not parse XML", pe);
		}
	}

}





