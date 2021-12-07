/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.dialect.Dialect;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Indicates that the annotated test class/method should be skipped
 * when the indicated Dialect is being used.
 *
 * It is a repeatable annotation
 *
 * @see SkipForDialectGroup
 *
 * @author Steve Ebersole
 */
@Inherited
@Retention( RetentionPolicy.RUNTIME )
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Repeatable( SkipForDialectGroup.class  )

@ExtendWith( DialectFilterExtension.class )
public @interface SkipForDialect {
	Class<? extends Dialect> dialectClass();
	boolean matchSubTypes() default false;
	String reason() default "<undefined>";

	int majorVersion() default -1;

	int minorVersion() default -1;

	int microVersion() default -1;
}
