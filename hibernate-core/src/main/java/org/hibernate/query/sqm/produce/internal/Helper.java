/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.query.sqm.ParsingException;

/**
 * @author Steve Ebersole
 */
public class Helper {
	public static Type toType(Bindable bindable) {
		switch ( bindable.getBindableType() ) {
			case ENTITY_TYPE: {
				return (EntityType) bindable;
			}
			case SINGULAR_ATTRIBUTE: {
				return ( (SingularAttribute) bindable ).getType();
			}
			case PLURAL_ATTRIBUTE: {
				return ( (PluralAttribute) bindable ).getElementType();
			}
			default: {
				throw new ParsingException( "Unexpected Bindable type : " + bindable );
			}
		}
	}

	private Helper() {
	}
}
