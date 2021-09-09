package org.hibernate.orm.test.mapping.converted.enums;

import java.util.Properties;

import org.hibernate.type.EnumType;

/**
 * A simple user type where we force enums to be saved by name, not ordinal
 * 
 * @author gtoison
 */
public class NamedEnumUserType<T extends Enum<T>> extends EnumType<T> {
	private static final long serialVersionUID = -4176945793071035928L;

	@Override
	public void setParameterValues(Properties parameters) {
		parameters.setProperty(EnumType.NAMED, "true");

		super.setParameterValues(parameters);
	}
}