/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;

/**
 * @author Steve Ebersole
 */
public interface BasicValuedModelPart extends BasicValuedMapping, ModelPart {
	/**
	 * The table expression (table name or subselect) that contains
	 * the {@linkplain #getMappedColumnExpression mapped column}
	 */
	String getContainingTableExpression();

	/**
	 * The column expression (column name or formula) to which this basic value
	 * is mapped
	 */
	String getMappedColumnExpression();

	/**
	 * Get the value converter applied to this model part if any
	 */
	BasicValueConverter getConverter();
}
