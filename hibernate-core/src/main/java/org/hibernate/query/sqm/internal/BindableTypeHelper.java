package org.hibernate.query.sqm.internal;

import org.hibernate.metamodel.mapping.Bindable;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

public final class BindableTypeHelper {

	private BindableTypeHelper() {
		//internal
	}

	/**
	 * @param o any object instance
	 * @return true iff the parameter o is of a type compatible with {@code Bindable}.
	 */
	public static Bindable asBindable(final Object o) {
		if ( o == null ) {
			return null;
		}
		else if ( o instanceof BasicTypeImpl ) {
			return (BasicTypeImpl) o;
		}
		else if ( o instanceof ConvertedBasicTypeImpl ) {
			return (ConvertedBasicTypeImpl) o;
		}
		else {
			if ( o instanceof Bindable ) {
				return (Bindable) o;
			}
			return null;
		}
	}
}
