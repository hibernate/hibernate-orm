/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.sql.ast.tree.spi.UpdateStatement;

/**
 * SqlAstDescriptor specialization for update queries
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
public interface SqlAstUpdateDescriptor extends SqlAstDescriptor {
	@Override
	UpdateStatement getSqlAstStatement();
}
