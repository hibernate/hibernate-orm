/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
