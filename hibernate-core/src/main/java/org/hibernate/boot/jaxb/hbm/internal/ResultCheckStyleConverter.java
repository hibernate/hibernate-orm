package org.hibernate.boot.jaxb.hbm.internal;

import org.hibernate.boot.jaxb.ResultCheckStyle;

/**
 * JAXB marshaling for the ExecuteUpdateResultCheckStyle enum
 *
 * @author Steve Ebersole
 */
public class ResultCheckStyleConverter {
	public static ResultCheckStyle fromXml(String name) {
		return ResultCheckStyle.fromExternalName( name );
	}

	public static String toXml(ResultCheckStyle style) {
		return style.externalName();
	}
}
