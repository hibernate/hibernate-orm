/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;

/**
 * @author Steve Ebersole
 */
public interface TableReferenceJoinCollector {
	void addPrimaryReference(TableReference root);
	void addSecondaryReference(TableReferenceJoin tableReferenceJoin);
}
