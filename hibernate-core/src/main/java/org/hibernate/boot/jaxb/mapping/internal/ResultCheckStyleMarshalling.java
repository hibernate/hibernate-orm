/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
		return name == null ? null : ExecuteUpdateResultCheckStyle.fromExternalName( name );
	}

	public static String toXml(ExecuteUpdateResultCheckStyle style) {
		return style == null ? null : style.externalName();
	}
}
