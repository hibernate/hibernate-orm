package org.hibernate.bytecode.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method that Hibernate's bytecode enhancement has generated.
 * The annotation is used by Jacoco to exclude generated methods from code coverage reports.
 * <p>
 * The {@code jakarta.annotation.Generated} annotation cannot be used for this purpose because it is not retained
 * in the class file.
 *
 * @see org.hibernate.bytecode.enhance.spi.Enhancer
 *
 * @author James Bodkin
 */
@Target({ ElementType.METHOD })
@Retention( RetentionPolicy.CLASS )
public @interface Generated {
}
