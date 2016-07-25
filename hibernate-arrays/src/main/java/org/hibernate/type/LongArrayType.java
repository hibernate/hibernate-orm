/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hibernate.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.LongArrayJavaDescriptor;
import org.hibernate.type.descriptor.sql.ArrayTypeDescriptor;

/**
 *
 * @author jordan
 */
public class LongArrayType 
		extends AbstractSingleColumnStandardBasicType<Long[]>
		implements LiteralType<Long[]> {

	public static final LongArrayType INSTANCE = new LongArrayType();

	public LongArrayType() {
		super(ArrayTypeDescriptor.INSTANCE, LongArrayJavaDescriptor.INSTANCE);
	}

	@Override
	public String getName() {
		return Long[].class.getName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public String objectToSQLString(Long[] value, Dialect dialect) throws Exception {
		StringBuilder sb = new StringBuilder("{");
		for (Long i : value) {
			sb.append(i).append(',');
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append('}');
		return sb.toString();
	}

	
}
