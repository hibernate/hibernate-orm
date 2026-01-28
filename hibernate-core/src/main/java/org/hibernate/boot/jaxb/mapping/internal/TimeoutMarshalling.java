/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import jakarta.persistence.Timeout;

/// JAXB marshaling for [Timeout]
///
/// @author Steve Ebersole
public class TimeoutMarshalling {
	public static Timeout fromXml(String value) {
		return value == null ? null : Timeout.seconds( Integer.parseInt( value ) );
	}

	public static String toXml(Timeout timeout) {
		return timeout == null ? null : Integer.toString( timeout.milliseconds() * 1000 );
	}
}
