/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hibernate.type.descriptor.java;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Calendar;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Java array type descriptor for the LocalDate type.
 */
public class LocalDateArrayJavaDescriptor extends AbstractArrayTypeDescriptor<LocalDate[]> {
	
	public static final LocalDateArrayJavaDescriptor INSTANCE = new LocalDateArrayJavaDescriptor();

	public LocalDateArrayJavaDescriptor() {
		super( LocalDate[].class, ArrayMutabilityPlan.INSTANCE );
	}
	
	@Override
	public String toString(LocalDate[] value) {
		if (value == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		String glue = "";
		for (LocalDate v : value) {
			sb.append(glue)
					.append('\'')
					.append(v)
					.append('\'');
			glue = ",";
		}
		sb.append('}');
		return sb.toString();
	}

	@Override
	public LocalDate[] fromString(String string) {
		if (string == null) {
			return null;
		}
		java.util.ArrayList<LocalDate> lst = new java.util.ArrayList<>();
		string = string.trim();
		StringBuilder sb = null;
		char lastchar = string.charAt(string.length() - 1);
		char opener;
		switch (lastchar) {
			case ']':
				opener = '[';
				break;
			case '}':
				opener = '{';
				break;
			case ')':
				opener = '(';
				break;
			default:
				throw new IllegalArgumentException("Cannot parse given string into array of strings");
		}
		int len = string.length(); // why call every time, if String is immutable?
		char[] duo = new char[2];
		int applen;
		for (int i = string.indexOf(opener) + 1; i < len; i++) {
			int cp = string.codePointAt(i);
			char quote;
			if (cp == '\'' || cp == '\"' || cp == '`') {
				quote = (char) cp;
			}
			else if (cp == lastchar) {
				// treat no-value between commas to mean null
				if (sb == null) {
					lst.add(null);
				}
				break;
			}
			else if (Character.isWhitespace(cp)) {
				continue;
			}
			else if (cp == ',') {
				// treat no-value between commas to mean null
				if (sb == null) {
					lst.add(null);
				}
				else {
					sb = null;
				}
				continue;
			}
			else if ("null".equalsIgnoreCase(string.substring(i, i + 4))) {
				// skip some possible whitespace
				int j = 5;
				do {
					cp = string.codePointAt(i + j);
					j++;
				}
				while(Character.isWhitespace(cp));
				// check if this was the last entry
				if (cp == lastchar || cp == ',') {
					lst.add(null);
					continue;
				}
				throw new IllegalArgumentException("Cannot parse given string into array of LocalDates");
			}
			else if (cp >= '0' && cp <='9') {
				// date not in quotes?
				String dateStr = string.substring(i, i + 10);
				lst.add(LocalDate.parse(dateStr));
				sb = new StringBuilder();
				continue;
			}
			else {
				throw new IllegalArgumentException("Cannot parse given string into array of LocalDates");
			}
			sb = new StringBuilder();
			while (++i < len && (cp = string.codePointAt(i)) != quote) {
				if (cp == '\\' && (i+1) < len && string.codePointAt(i) == quote) {
					sb.appendCodePoint(quote);
					continue;
				}
				sb.appendCodePoint(cp);
				if (!Character.isBmpCodePoint(cp)) {
					i++;
				}
			}
			lst.add(LocalDate.parse(sb.toString()));
		}
		return lst.toArray(new LocalDate[lst.size()]);
	}

	@Override
	public <X> X unwrap(LocalDate[] value, Class<X> type, WrapperOptions options) {
		// function used for PreparedStatement binding

		if ( value == null ) {
			return null;
		}

		if ( java.sql.Array.class.isAssignableFrom( type ) ) {
			Dialect sqlDialect;
			java.sql.Connection conn;
			if (!(options instanceof SharedSessionContractImplementor)) {
				throw new IllegalStateException("You can't handle the truth! I mean arrays...");
			}
			SharedSessionContractImplementor sess = (SharedSessionContractImplementor) options;
			sqlDialect = sess.getJdbcServices().getDialect();
			try {
				conn = sess.getJdbcConnectionAccess().obtainConnection();
				String typeName = sqlDialect.getTypeName(java.sql.Types.DATE);
				java.sql.Date[] converted = new java.sql.Date[value.length];
				for (int i = 0; i < value.length; i++) {
					LocalDate ld = value[i];
					// convert to idiotic legacy date API
					java.util.Calendar cal = java.util.Calendar.getInstance();
					cal.set(Calendar.MILLISECOND, 0);
					cal.set(ld.getYear(), ld.getMonth().getValue() - 1, ld.getDayOfMonth(), 0, 0, 0);
					converted[i] = new java.sql.Date(cal.getTimeInMillis());
				}
				return (X) conn.createArrayOf(typeName, value);
			}
			catch (SQLException ex) {
				// This basically shouldn't happen unless you've lost connection to the database.
				throw new HibernateException(ex);
			}
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> LocalDate[] wrap(X value, WrapperOptions options) {
		// function used for ResultSet extraction

		if ( value == null ) {
			return null;
		}

		if ( value instanceof LocalDate[]) {
			return (LocalDate[]) value;
		}

		if ( ! ( value instanceof java.sql.Array ) ) {
			throw unknownWrap ( value.getClass() );
		}

		java.sql.Array original = (java.sql.Array) value;
		try {
			Object raw = original.getArray();
			Class clz = raw.getClass().getComponentType();
			if (clz == null || !clz.getName().equals("java.lang.String")) {
				throw unknownWrap ( clz );
			}
			return (LocalDate[]) raw;
		}
		catch (SQLException ex) {
			// This basically shouldn't happen unless you've lost connection to the database.
			throw new HibernateException(ex);
		}

	}
	
}
