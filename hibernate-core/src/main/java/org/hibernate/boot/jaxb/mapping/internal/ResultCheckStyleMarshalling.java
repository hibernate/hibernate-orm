/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;

/**
 * JAXB marshalling for {@link ExecuteUpdateResultCheckStyle}
 *
 * @author Steve Ebersole
 */
public class ResultCheckStyleMarshalling {
	public static ExecuteUpdateResultCheckStyle fromXml(String name) {
		return name == null ? null : ExecuteUpdateResultCheckStyle.valueOf( name );
	}

	public static String toXml(ExecuteUpdateResultCheckStyle style) {
		return style == null ? null : style.name();
	}
}
