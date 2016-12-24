/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.convert.results.internal;

import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.ast.expression.instantiation.DynamicInstantiation;
import org.hibernate.sql.convert.results.spi.ReturnDynamicInstantiation;
import org.hibernate.sql.exec.results.process.spi.ReturnAssembler;

/**
 * @author Steve Ebersole
 */
public class ReturnDynamicInstantiationImpl implements ReturnDynamicInstantiation {
	private final DynamicInstantiation dynamicInstantiation;
	private final String resultVariable;
	private final ReturnAssembler assembler;

	public ReturnDynamicInstantiationImpl(
			DynamicInstantiation dynamicInstantiation,
			String resultVariable,
			ReturnAssembler assembler) {
		this.dynamicInstantiation = dynamicInstantiation;
		this.resultVariable = resultVariable;
		this.assembler = assembler;
	}

	@Override
	public Class getInstantiationTarget() {
		return dynamicInstantiation.getTarget();
	}

	@Override
	public Expression getSelectedExpression() {
		return dynamicInstantiation;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public Class getReturnedJavaType() {
		return dynamicInstantiation.getTarget();
	}

	@Override
	public ReturnAssembler getReturnAssembler() {
		return assembler;
	}
}
