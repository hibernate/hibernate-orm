/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import org.hibernate.naming.spi.RelationalNamespace;

/**
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface Namespace extends RelationalNamespace<Table,Sequence> {
	// probably need some notion of "auxiliary database object"
}
