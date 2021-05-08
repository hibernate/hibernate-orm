/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.NumericBooleanType;
import org.hibernate.type.TrueFalseType;
import org.hibernate.type.YesNoType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Simple configuration of built-in boolean mapping strategies
 *
 * @see org.hibernate.type.YesNoConverter
 * @see org.hibernate.type.TrueFalseConverter
 * @see org.hibernate.type.NumericBooleanConverter
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface BooleanMapping {
	enum Style {
		BOOLEAN( null ),
		Y_N( YesNoType.YesNoConverter.INSTANCE  ),
		T_F( TrueFalseType.TrueFalseConverter.INSTANCE ),
		NUMERIC( NumericBooleanType.NumericConverter.INSTANCE );

		private final BasicValueConverter<Boolean,?> impliedConversion;

		Style(BasicValueConverter<Boolean, ?> impliedConversion) {
			this.impliedConversion = impliedConversion;
		}

		public BasicValueConverter<Boolean, ?> getImpliedConversion() {
			return impliedConversion;
		}
	}

	Style style() default Style.BOOLEAN;
}
