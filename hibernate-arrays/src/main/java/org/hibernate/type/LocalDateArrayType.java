/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hibernate.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.LocalDateArrayJavaDescriptor;
import org.hibernate.type.descriptor.sql.ArrayTypeDescriptor;

import java.time.LocalDate;

/**
 *
 * @author jordan
 */
public class LocalDateArrayType
		extends AbstractSingleColumnStandardBasicType<LocalDate[]>
		implements LiteralType<LocalDate[]> {

	public static final LocalDateArrayType INSTANCE = new LocalDateArrayType();
	public LocalDateArrayType() {
		super(ArrayTypeDescriptor.INSTANCE, LocalDateArrayJavaDescriptor.INSTANCE);
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
	public String objectToSQLString(LocalDate[] value, Dialect dialect) throws Exception {
		if (value == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		String glue = "";
		for (LocalDate v : value) {
			sb.append(glue);
			if ( v == null ) {
				sb.append("null");
				glue = ",";
				continue;
			}
			sb.append('\'')
					.append(v.toString())
					.append('\'');
			glue = ",";
		}
		sb.append('}');
		return sb.toString();
	}

}
