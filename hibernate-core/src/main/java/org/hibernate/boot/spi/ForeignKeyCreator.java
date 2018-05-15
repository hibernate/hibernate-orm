/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.spi;

import java.util.function.Function;

import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;

/**
 * @author Steve Ebersole
 */
public interface ForeignKeyCreator extends Function<RuntimeModelCreationContext,Boolean> {
	@Override
	default Boolean apply(RuntimeModelCreationContext creationContext) {
		return createForeignKey( creationContext );
	}

	boolean createForeignKey(RuntimeModelCreationContext creationContext);
}
