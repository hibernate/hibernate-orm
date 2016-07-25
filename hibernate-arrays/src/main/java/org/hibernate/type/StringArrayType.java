
package org.hibernate.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.StringArrayJavaDescriptor;
import org.hibernate.type.descriptor.sql.ArrayTypeDescriptor;

public class StringArrayType
		extends AbstractSingleColumnStandardBasicType<String[]>
		implements LiteralType<String[]> {

	public static final StringArrayType INSTANCE = new StringArrayType();

	public StringArrayType() {
		super(ArrayTypeDescriptor.INSTANCE, StringArrayJavaDescriptor.INSTANCE);
	}

	@Override
	public String getName() {
		return String[].class.getName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public String objectToSQLString(String[] value, Dialect dialect) throws Exception {
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

}
