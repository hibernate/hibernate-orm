/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

/**
 * Something that can be expressable at the mapping model level.
 *
 * Generally this is used generation of SQL AST
 *
 * todo (6.0) : Better name?  This one's a bit verbose.  See description for clues
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface MappingModelExpressable<T> extends Bindable {
}
