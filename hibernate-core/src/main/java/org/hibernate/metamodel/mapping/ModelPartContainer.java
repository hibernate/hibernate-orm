/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;

/**
 * Access to a group of ValueMappings by name or for iteration
 *
 * @author Steve Ebersole
 */
public interface ModelPartContainer extends ModelPart {
	/**
	 * Find a sub-ValueMapping by name
	 */
	default ModelPart findSubPart(String name){
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Visit all of this container's sub-ValueMappings
	 */
	default void visitSubParts(Consumer<ModelPart> consumer){
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	// todo (6.0) : consider for SQM -> SQL conversion :
	//		````
	// 		ColumnReferenceQualifier resolveColumnReferenceQualifier(String name);
	//		````
	// 	 - the one concern to that is properly handling needing the join versus not needing it wrt joinable fk references
}
