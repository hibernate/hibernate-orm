package org.hibernate.orm.test.mapping.attributebinder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.annotations.AttributeBinderType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Dummy annotation to verify binders are called only once.
 *
 * @author Yanming Zhou
 */
@Target({METHOD,FIELD})
@Retention(RUNTIME)
@AttributeBinderType( binder = FooBinder.class )
public @interface Foo {
}
