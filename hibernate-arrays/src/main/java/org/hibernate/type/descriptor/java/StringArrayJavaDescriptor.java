
package org.hibernate.type.descriptor.java;

import java.sql.SQLException;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Java array type descriptor for the String type.
 */
public class StringArrayJavaDescriptor extends AbstractArrayTypeDescriptor<String[]> {
	
	public static final StringArrayJavaDescriptor INSTANCE = new StringArrayJavaDescriptor();

	@SuppressWarnings("unchecked")
	public StringArrayJavaDescriptor() {
		super( String[].class, ArrayMutabilityPlan.INSTANCE );
	}

	@Override
	public String toString(String[] value) {
		if (value == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		String glue = "";
		for (String v : value) {
			sb.append(glue);
			if ( v == null ) {
				sb.append("null");
				glue = ",";
				continue;
			}
			sb.append('\'');
			int cp;
			int len = v.length();
			for (int i = 0; i < len; i++) {
				cp = v.codePointAt(i);
				if (i == '\'') {
					sb.append('\\');
				}
				else if (!Character.isBmpCodePoint(cp)) {
					i++;
				}
				sb.appendCodePoint(cp);
			}
			sb.append('\'');
			glue = ",";
		}
		sb.append('}');
		return sb.toString();
	}

	@Override
	public String[] fromString(String string) {
		if (string == null) {
			return null;
		}
		java.util.ArrayList<String> lst = new java.util.ArrayList<>();
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
				throw new IllegalArgumentException("Cannot parse given string into array of strings");
			}
			else {
				throw new IllegalArgumentException("Cannot parse given string into array of strings");
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
			lst.add(sb.toString());
		}
		return lst.toArray(new String[lst.size()]);
	}

	@Override
	public <X> X unwrap(String[] value, Class<X> type, WrapperOptions options) {
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
				String typeName = sqlDialect.getTypeName(java.sql.Types.LONGVARCHAR);
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
	public <X> String[] wrap(X value, WrapperOptions options) {
		// function used for ResultSet extraction

		if ( value == null ) {
			return null;
		}

		if ( value instanceof String[]) {
			return (String[]) value;
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
			return (String[]) raw;
		}
		catch (SQLException ex) {
			// This basically shouldn't happen unless you've lost connection to the database.
			throw new HibernateException(ex);
		}

	}
	
}
