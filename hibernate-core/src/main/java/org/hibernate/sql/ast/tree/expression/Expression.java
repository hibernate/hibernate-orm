/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionProducer;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Models an expression at the SQL-level.
 *
 * @author Steve Ebersole
 */
public interface Expression extends SqlAstNode, SqlSelectionProducer {
	//
	/**
	 * Access the type for this expression.  See {@link ExpressableType}
	 * for more detailed description.
	 *
	 * todo (6.0) : given the move to have NavigableReference implement Expression,
	 * 		it might be better to define this type in terms of DomainTypeDescriptor
	 */
	SqlExpressableType getType();

	/**
	 * If this expression is used as a selection in the SQL this method
	 * will be called to generate the corresponding SqlSelection (reader,
	 * position, etc) that can be used to read its value.
	 */
	@Override
	SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration);
}
