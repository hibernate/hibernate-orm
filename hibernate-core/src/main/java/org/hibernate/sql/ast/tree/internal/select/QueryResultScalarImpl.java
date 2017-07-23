/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.internal.select;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.tree.spi.select.QueryResultScalar;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.sql.ast.tree.spi.select.SqlSelection;
import org.hibernate.sql.exec.results.internal.QueryResultAssemblerScalar;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class QueryResultScalarImpl implements QueryResultScalar {
	private final Selection selection;
	private final BasicValuedExpressableType expressableType;

	private final QueryResultAssembler assembler;


	public QueryResultScalarImpl(
			Selection selection,
			SqlSelection sqlSelection,
			BasicValuedExpressableType expressableType) {
		// note : just a single SqlSelection because this describes a scalar return

		this.selection = selection;
		this.expressableType = expressableType;

		this.assembler = new QueryResultAssemblerScalar( sqlSelection, this );
	}

	@Override
	public Selection getSelection() {
		return selection;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getType().getJavaTypeDescriptor();
	}

	@Override
	public ExpressableType getType() {
		return expressableType;
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		return assembler;
	}
}
