/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQueryDelete;

/**
 * @author Steve Ebersole
 */
public class SimpleDeleteQueryPlan extends AbstractDeleteQueryPlan<DeleteStatement, JdbcOperationQueryDelete> {
	public SimpleDeleteQueryPlan(
			EntityMappingType entityDescriptor,
			SqmDeleteStatement<?> sqmDelete,
			DomainParameterXref domainParameterXref) {
		super( entityDescriptor, sqmDelete, domainParameterXref );
	}

	@Override
	protected DeleteStatement buildAst(
			SqmTranslation<DeleteStatement> sqmInterpretation,
			DomainQueryExecutionContext executionContext) {
		return sqmInterpretation.getSqlAst();
	}

	@Override
	protected SqlAstTranslator<JdbcOperationQueryDelete> createTranslator(
			DeleteStatement ast,
			DomainQueryExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();
		return factory.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildDeleteTranslator( factory, ast );
	}

}
