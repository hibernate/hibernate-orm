/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.query.sqm.internal;

import java.util.Collections;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQueryUpdate;

/**
 * NonSelectQueryPlan for handling DELETE queries against an entity with soft-delete
 *
 * @author Steve Ebersole
 */
public class SoftDeleteQueryPlan extends AbstractDeleteQueryPlan<UpdateStatement,JdbcOperationQueryUpdate> {
	public SoftDeleteQueryPlan(
			EntityMappingType entityDescriptor,
			SqmDeleteStatement<?> sqmDelete,
			DomainParameterXref domainParameterXref) {
		super( entityDescriptor, sqmDelete, domainParameterXref );
		assert entityDescriptor.getSoftDeleteMapping() != null;
	}

	@Override
	protected UpdateStatement buildAst(
			SqmTranslation<DeleteStatement> sqmInterpretation,
			DomainQueryExecutionContext executionContext) {
		final DeleteStatement sqlDeleteAst = sqmInterpretation.getSqlAst();
		final NamedTableReference targetTable = sqlDeleteAst.getTargetTable();
		final SoftDeleteMapping columnMapping = getEntityDescriptor().getSoftDeleteMapping();
		final ColumnReference columnReference = new ColumnReference( targetTable, columnMapping );
		//noinspection rawtypes,unchecked
		final JdbcLiteral jdbcLiteral = new JdbcLiteral( columnMapping.getDeletedLiteralValue(), columnMapping.getJdbcMapping() );
		final Assignment assignment = new Assignment( columnReference, jdbcLiteral );

		return new UpdateStatement(
				targetTable,
				Collections.singletonList( assignment ),
				sqlDeleteAst.getRestriction()
		);
	}

	@Override
	protected SqlAstTranslator<JdbcOperationQueryUpdate> createTranslator(
			UpdateStatement sqlUpdateAst,
			DomainQueryExecutionContext executionContext) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor factory = session.getFactory();
		return factory.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildUpdateTranslator( factory, sqlUpdateAst );
	}
}
