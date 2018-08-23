package org.hibernate.test.type.array;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;

/**
 * @author Vlad Mihalcea
 */
public class StringArrayType
		extends AbstractSingleColumnStandardBasicType<String[]> {

	public static final StringArrayType INSTANCE = new StringArrayType();

	public StringArrayType() {
		super( StringArraySqlTypeDescriptor.INSTANCE, StringArrayTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "string-array";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
