/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.convert.results.internal;

import java.util.List;

import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.ast.select.SelectableEmbeddedTypeImpl;
import org.hibernate.sql.ast.select.SqlSelection;
import org.hibernate.sql.convert.results.spi.ReturnComposite;
import org.hibernate.sql.exec.results.process.internal.ReturnAssemblerComposite;
import org.hibernate.sql.exec.results.process.spi.ReturnAssembler;
import org.hibernate.type.spi.CompositeType;

/**
 * @author Steve Ebersole
 */
public class ReturnCompositeImpl implements ReturnComposite {
	private final SelectableEmbeddedTypeImpl selectableEmbeddedType;
	private final String resultVariable;
	private final CompositeType compositeType;

	private final ReturnAssembler assembler;

	public ReturnCompositeImpl(
			SelectableEmbeddedTypeImpl selectableEmbeddedType,
			String resultVariable,
			List<SqlSelection> sqlSelections,
			CompositeType compositeType) {
		this.selectableEmbeddedType = selectableEmbeddedType;
		this.resultVariable = resultVariable;
		this.compositeType = compositeType;

		this.assembler = new ReturnAssemblerComposite(
				this,
				sqlSelections,
				compositeType
		);
	}

	@Override
	public Expression getSelectedExpression() {
		return selectableEmbeddedType.getSelectedExpression();
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public Class getReturnedJavaType() {
		return compositeType.getReturnedClass();
	}

	@Override
	public ReturnAssembler getReturnAssembler() {
		return assembler;
	}
}
