package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.annotations.NotFoundAction;

/**
 * @author Steve Ebersole
 */
public class NotFoundActionMarshalling {
	public static NotFoundAction fromXml(String name) {
		return name == null ? null : NotFoundAction.valueOf( name );
	}

	public static String toXml(NotFoundAction action) {
		return action == null ? null : action.name();
	}
}
