/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.spi;

import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.consume.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlAstUpdateDescriptor;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * @author Steve Ebersole
 */
public class SqmDeleteToSqlAstConverterMultiTable extends BaseSqmToSqlAstConverter {

	private final QuerySpec idTableSelect;
	private final EntityTypeDescriptor entityDescriptor;
	private final TableGroup entityTableGroup;

	public static List<SqlAstUpdateDescriptor> interpret(
			SqmDeleteStatement sqmStatement,
			QuerySpec idTableSelect,
			QueryOptions queryOptions,
			SqlAstCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception();

//		final SqmDeleteToSqlAstConverterMultiTable walker = new SqmDeleteToSqlAstConverterMultiTable(
//				sqmStatement,
//				idTableSelect,
//				queryOptions,
//				creationContext
//		);
//
//		walker.visitDeleteStatement( sqmStatement );
//
//		// see SqmUpdateToSqlAstConverterMultiTable#interpret
//		return walker.updateStatementBuilderMap.entrySet().stream()
//				.map( entry -> entry.getValue().createUpdateDescriptor() )
//				.collect( Collectors.toList() );
	}

	public SqmDeleteToSqlAstConverterMultiTable(
			SqmDeleteStatement sqmStatement,
			QuerySpec idTableSelect,
			QueryOptions queryOptions,
			SqlAstCreationContext creationContext) {
		super( creationContext, queryOptions, LoadQueryInfluencers.NONE, afterLoadAction -> {} );
		this.idTableSelect = idTableSelect;

		final SqmRoot deleteTarget = sqmStatement.getTarget();

		this.entityDescriptor = deleteTarget.getReferencedNavigable().getEntityDescriptor();

		this.entityTableGroup = entityDescriptor.createRootTableGroup(
				deleteTarget.getUniqueIdentifier(),
				deleteTarget.getNavigablePath(),
				deleteTarget.getExplicitAlias(),
				JoinType.INNER,
				queryOptions.getLockOptions().getLockMode(),
				this
		);

		getFromClauseIndex().crossReference( deleteTarget, entityTableGroup );
	}

	@Override
	public Object visitDeleteStatement(SqmDeleteStatement statement) {
		return super.visitDeleteStatement( statement );
	}
}
