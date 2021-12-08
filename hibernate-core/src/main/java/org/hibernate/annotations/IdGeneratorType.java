/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.id.IdentifierGenerator;

/**
 * Meta-annotation used to mark another annotation as providing configuration
 * for a custom {@link org.hibernate.id.IdentifierGenerator}.
 */
@Target( value = ElementType.ANNOTATION_TYPE )
@Retention( RetentionPolicy.RUNTIME )
public @interface IdGeneratorType {
	/**
	 * The IdentifierGenerator being configured
	 */
	Class<? extends IdentifierGenerator> value();
}
