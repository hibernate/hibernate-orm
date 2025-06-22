/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.internal;

import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;

/**
 * JAXB marshalling for the ExecuteUpdateResultCheckStyle enum
 *
 * @author Steve Ebersole
 */
public class ExecuteUpdateResultCheckStyleConverter {
	public static ExecuteUpdateResultCheckStyle fromXml(String name) {
		return ExecuteUpdateResultCheckStyle.fromExternalName( name );
	}

	public static String toXml(ExecuteUpdateResultCheckStyle style) {
		return style.externalName();
	}
}
