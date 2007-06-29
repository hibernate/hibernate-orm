//$Id: ImmutableType.java 5744 2005-02-16 12:50:19Z oneovthafew $
package org.hibernate.type;

import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;

/**
 * Superclass of nullable immutable types.
 * @author Gavin King
 */
public abstract class ImmutableType extends NullableType {

	public final Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory) {
		return value;
	}

	public final boolean isMutable() {
		return false;
	}

	public Object replace(
		Object original,
		Object target,
		SessionImplementor session,
		Object owner, 
		Map copyCache)
	throws HibernateException {
		return original;
	}


}
