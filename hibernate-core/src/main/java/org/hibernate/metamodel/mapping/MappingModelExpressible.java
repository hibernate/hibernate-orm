/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

/**
 * Something that can be expressible at the mapping model level.
 *
 * Generally this is used generation of SQL AST
 *
 * todo (6.0) : Better name?  This one's a bit verbose.  See description for clues
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface MappingModelExpressible<T> extends Bindable {
}
