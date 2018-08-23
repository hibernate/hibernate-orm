package org.hibernate.test.type.array;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
public class StringArrayTypeDescriptor
		extends AbstractTypeDescriptor<String[]> {

	public static final StringArrayTypeDescriptor INSTANCE = new StringArrayTypeDescriptor();

	public StringArrayTypeDescriptor() {
		super( String[].class );
	}

	public boolean areEqual(String[] one, String[] another) {
		if ( one == another ) {
			return true;
		}
		return !( one == null || another == null ) && Arrays.equals( one, another );
	}

	public String toString(String[] value) {
		return Arrays.deepToString( value );
	}

	@Override
	public String[] fromString(String string) {
		return null;
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(String[] value, Class<X> type, WrapperOptions options) {
		return (X) value;
	}

	@Override
	public <X> String[] wrap(X value, WrapperOptions options) {
		if ( value instanceof Array ) {
			Array array = (Array) value;
			try {
				return (String[]) array.getArray();
			}
			catch (SQLException e) {
				throw new IllegalArgumentException( e );
			}
		}
		return (String[]) value;
	}

	public String getSqlArrayType() {
		return "varchar";
	}
}
