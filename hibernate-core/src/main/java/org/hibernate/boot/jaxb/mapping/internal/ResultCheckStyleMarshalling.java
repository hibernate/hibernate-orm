package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.boot.jaxb.ResultCheckStyle;

/**
 * JAXB marshaling for {@link ResultCheckStyle}
 *
 * @author Steve Ebersole
 */
public class ResultCheckStyleMarshalling {
	public static ResultCheckStyle fromXml(String name) {
		return name == null ? null : ResultCheckStyle.valueOf( name );
	}

	public static String toXml(ResultCheckStyle style) {
		return style == null ? null : style.name();
	}
}
