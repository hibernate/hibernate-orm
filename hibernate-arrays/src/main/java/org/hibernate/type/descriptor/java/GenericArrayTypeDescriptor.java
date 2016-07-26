package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.sql.SQLException;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.SessionImpl;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.descriptor.WrapperOptions;

public class GenericArrayTypeDescriptor<T> extends AbstractArrayTypeDescriptor<T[]> {

	private final JavaTypeDescriptor<T> componentDescriptor;
	private final Class<T> componentClass;
	private final MutabilityPlan<T[]> mutaplan;
	private final int sqlType;

	public GenericArrayTypeDescriptor(AbstractStandardBasicType<T> baseDescriptor) {
		super( (Class<T[]>) Array.newInstance(baseDescriptor.getJavaTypeDescriptor().getJavaTypeClass(), 0).getClass() );
		this.componentDescriptor = baseDescriptor.getJavaTypeDescriptor();
		this.componentClass = baseDescriptor.getJavaTypeDescriptor().getJavaTypeClass();
		if (this.componentClass.isArray()) {
			this.mutaplan = new LocalArrayMutabilityPlan(this.componentDescriptor.getMutabilityPlan());
		}
		else {
			this.mutaplan = ArrayMutabilityPlan.INSTANCE;
		}
		this.sqlType = baseDescriptor.getSqlTypeDescriptor().getSqlType();
	}

	private class LocalArrayMutabilityPlan implements MutabilityPlan<T[]> {
		MutabilityPlan<T> superplan;
		public LocalArrayMutabilityPlan(MutabilityPlan<T> superplan) {
			this.superplan = superplan;
		}

		@Override
		public boolean isMutable() {
			return superplan.isMutable();
		}

		@Override
		public T[] deepCopy(T[] value) {
			if (value == null) {
				return null;
			}
			T[] copy = (T[]) Array.newInstance(componentClass, value.length);
			for (int i = 0; i < value.length; i++) {
				copy[i] = superplan.deepCopy(value[i]);
			}
			return copy;
		}

		@Override
		public Serializable disassemble(T[] value) {
			return (Serializable) deepCopy( value );
		}

		@Override
		public T[] assemble(Serializable cached) {
			return deepCopy ((T[]) cached);
		}
		
	}

	@Override
	public MutabilityPlan<T[]> getMutabilityPlan() {
		return super.getMutabilityPlan();
	}

	@Override
	public String toString(T[] value) {
		if (value == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		String glue = "";
		for (T v : value) {
			sb.append(glue);
			if ( v == null ) {
				sb.append("null");
				glue = ",";
				continue;
			}
			sb.append('\'');
			sb.append(v.toString().replace("\\", "\\\\").replace("'", "\\'"));
			sb.append('\'');
			glue = ",";
		}
		sb.append('}');
		return sb.toString();
	}

	@Override
	public T[] fromString(String string) {
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
				if (cp == '\\' && (i+1) < len) {
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
		T[] result = (T[]) Array.newInstance(componentClass, lst.size());
		for (int i = 0; i < result.length; i++) {
			result[i] = componentDescriptor.fromString(lst.get(i));
		}
		return result;
	}

	@Override
	public <X> X unwrap(T[] value, Class<X> type, WrapperOptions options) {
		// function used for PreparedStatement binding

		if ( value == null ) {
			return null;
		}

		if ( java.sql.Array.class.isAssignableFrom( type ) ) {
			Dialect sqlDialect;
			java.sql.Connection conn;
			if (!(options instanceof SessionImpl)) {
				throw new IllegalStateException("You can't handle the truth! I mean arrays...");
			}
			SessionImpl sess = (SessionImpl) options;
			sqlDialect = sess.getJdbcServices().getDialect();
			try {
				conn = sess.getJdbcConnectionAccess().obtainConnection();
				String typeName = sqlDialect.getTypeName(sqlType);
				int cutIndex = typeName.indexOf('(');
				if (cutIndex > 0) {
					// getTypeName for this case required length, etc, parameters.
					// Cut them out and use database defaults.
					typeName = typeName.substring(0, cutIndex);
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
	public <X> T[] wrap(X value, WrapperOptions options) {
		// function used for ResultSet extraction

		if ( value == null ) {
			return null;
		}

		if ( ! ( value instanceof java.sql.Array ) ) {
			throw unknownWrap ( value.getClass() );
		}

		java.sql.Array original = (java.sql.Array) value;
		try {
			Object raw = original.getArray();
			Class clz = raw.getClass().getComponentType();
			if (clz == null || !clz.getName().equals(componentClass.getName())) {
				throw unknownWrap ( raw.getClass() );
			}
			return (T[]) raw;
		}
		catch (SQLException ex) {
			// This basically shouldn't happen unless you've lost connection to the database.
			throw new HibernateException(ex);
		}
	}
}
