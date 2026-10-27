package org.hibernate.metamodel.mapping;

import org.hibernate.type.descriptor.java.JavaTypedExpressible;

/**
 * Something that can be expressible at the mapping model level.
 *
 * Generally this is used generation of SQL AST
 *
 * todo (6.0) : Better name?  This one's a bit verbose.  See description for clues
 *
 * @param <T> the Java type represented by this mapping in an expression
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface MappingModelExpressible<T> extends Bindable, JavaTypedExpressible<T> {
}
