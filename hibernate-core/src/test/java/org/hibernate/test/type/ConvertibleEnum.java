package org.hibernate.test.type;

import javax.persistence.Convert;

/**
 * @author Oleksander Dukhno
 */
@Convert(converter = EnumConverter.class)
public enum ConvertibleEnum {
	VALUE,
	DEFAULT;

	public String convertToString() {
		switch ( this ) {
			case VALUE:
				return "VALUE";
			default:
				return "DEFAULT";
		}
	}
}
