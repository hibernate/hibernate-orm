package org.hibernate.internal.build;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

@Retention( RetentionPolicy.CLASS )
@Target({ TYPE, METHOD, CONSTRUCTOR })
public @interface AllowReflection {
}
