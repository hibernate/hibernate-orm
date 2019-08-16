/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

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
public interface MappingModelExpressable<T> {
	JavaTypeDescriptor<T> getExpressableJavaTypeDescriptor();

	// todo (6.0) : others?
	//  		Probably `org.hibernate.metamodel.mapping.Bindable` should be consumed here.  Or at least exposed from here
	//
	// todo (6.0) : IMO `Bindable` should be consumed here and `Bindable` go away

	void visitJdbcTypes(Consumer<SqlExpressableType> action, TypeConfiguration typeConfiguration);

	default Bindable getBindable() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
