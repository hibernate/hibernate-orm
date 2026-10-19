package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.annotations.SourceType;

/**
 * JAXB marshalling for {@link SourceType}
 *
 * @author Andrea Boriero
 */
public class SourceTypeMarshalling {
	public static SourceType fromXml(String name) {
		return name == null ? null : SourceType.valueOf( name );
	}

	public static String toXml(SourceType sourceType) {
		return sourceType == null ? null : sourceType.name();
	}
}
