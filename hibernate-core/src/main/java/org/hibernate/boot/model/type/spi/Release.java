/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotate a method on a {@link BasicTypeProducer} implementation
 * that should be called when we release the {@link BasicTypeProducerRegistry}.
 *
 * @author Steve Ebersole
 */
@Retention(RetentionPolicy.RUNTIME )
@Target(ElementType.METHOD)
public @interface Release {
}
