/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query.results.internal;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultSetMappingResolutionContext;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 */
public class HbmForeignKeyRefMemento implements FetchMemento {

	public static HbmForeignKeyRefMemento createKeySideRef(NavigablePath navigablePath, ForeignKeyDescriptor fkDescriptor, List<String> columnAliases) {
		return new HbmForeignKeyRefMemento(
				navigablePath,
				fkDescriptor,
				fkDescriptor.getKeySide(),
				columnAliases
		);
	}

	/**
	 * Same as {@link #createKeySideRef}
	 */
	public static HbmForeignKeyRefMemento createReferringSideRef(NavigablePath navigablePath, ForeignKeyDescriptor fkDescriptor, List<String> columnAliases) {
		return createKeySideRef( navigablePath, fkDescriptor, columnAliases );
	}

	public static HbmForeignKeyRefMemento createTargetSideRef(NavigablePath navigablePath, ForeignKeyDescriptor fkDescriptor, List<String> columnAliases) {
		return new HbmForeignKeyRefMemento(
				navigablePath,
				fkDescriptor,
				fkDescriptor.getTargetSide(),
				columnAliases
		);
	}

	private final Builder builder;

	public HbmForeignKeyRefMemento(
			NavigablePath navigablePath,
			ForeignKeyDescriptor fkDescriptor,
			ForeignKeyDescriptor.Side side,
			List<String> columnAliases) {
		this.builder = new Builder( navigablePath, fkDescriptor, side, columnAliases );
	}

	public HbmForeignKeyRefMemento(
			NavigablePath navigablePath,
			ForeignKeyDescriptor fkDescriptor,
			ForeignKeyDescriptor.Nature nature,
			List<String> columnAliases) {
		this( navigablePath, fkDescriptor, fkDescriptor.getSide( nature ), columnAliases );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return builder.navigablePath;
	}

	@Override
	public FetchBuilder resolve(Parent parent, Consumer<String> querySpaceConsumer, ResultSetMappingResolutionContext context) {
		return builder;
	}


	private static class Builder implements FetchBuilder {
		private final NavigablePath navigablePath;
		private final ForeignKeyDescriptor fkDescriptor;
		private final ForeignKeyDescriptor.Side side;
		private final List<String> columnAliases;

		public Builder(
				NavigablePath navigablePath,
				ForeignKeyDescriptor fkDescriptor,
				ForeignKeyDescriptor.Side side,
				List<String> columnAliases) {
			this.navigablePath = navigablePath;
			this.fkDescriptor = fkDescriptor;
			this.side = side;
			this.columnAliases = columnAliases;
		}

		@Override
		public Fetch buildFetch(
				FetchParent parent,
				NavigablePath fetchPath,
				JdbcValuesMetadata jdbcResultsMetadata,
				BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
				DomainResultCreationState creationState) {

			final SessionFactoryImplementor sessionFactory = creationState.getSqlAstCreationState()
					.getCreationContext()
					.getSessionFactory();
			final boolean hasExplicitColumnAliases = CollectionHelper.isNotEmpty( columnAliases );

			final FromClauseAccess fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();
			final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlAstCreationState().getSqlExpressionResolver();

			final ModelPart fkPart = side.getModelPart();
			final String targetTable = side.getNature() == ForeignKeyDescriptor.Nature.TARGET ? fkDescriptor.getTargetTable() : fkDescriptor.getKeyTable();
			final TableGroup collectionTableGroup = fromClauseAccess.getTableGroup( parent.getNavigablePath() );
			final TableReference tableReference = collectionTableGroup.resolveTableReference( navigablePath, targetTable );

			final List<JdbcMapping> jdbcMappings = fkPart.getJdbcMappings();

			fkPart.forEachSelectable( (position, selectable) -> {
				final JdbcMapping jdbcMapping = jdbcMappings.get( position );

				// note : the model parts will be looking for columns based on the mapped
				// 		names so the expression key is built using the mapped column.
				//		but the actual ColumnReference is built using the name we expect to
				//		find in the ResultSet
				final String explicitColumnAlias = hasExplicitColumnAliases ? columnAliases.get( position ) : null;
				final String columnAlias = explicitColumnAlias != null
						? explicitColumnAlias
						: selectable.getSelectionExpression();

				assert columnAlias.equals( jdbcResultsMetadata.resolveColumnName( position ) );

				final Expression expression = sqlExpressionResolver.resolveSqlExpression(
						createColumnReferenceKey( tableReference, selectable ),
						(processingState) -> new ColumnReference(
								tableReference,
								columnAlias,
								jdbcMapping,
								sessionFactory
						)
				);

				sqlExpressionResolver.resolveSqlSelection(
						expression,
						jdbcMapping.getJavaTypeDescriptor(),
						sessionFactory.getTypeConfiguration()
				);

			} );

			// todo (6.0) - resolve TableGroupJoin
			//		- maybe a psuedo-TableGroupJoin based on just the "declaring" table?
			//		- maybe even add this as a method on ForeignKeyDescriptor/Side?

//			final TableGroupJoinProducer joinProducer = (TableGroupJoinProducer) pluralAttr.getDeclaringType();
//
//			fromClauseAccess.resolveTableGroup( navigablePath, (np) -> {
//				pluralAttr.getDeclaringType()
//				if ( pluralAttr.getElementDescriptor() instanceof TableGroupJoinProducer ) {
//					final TableGroupJoinProducer joinProducer = (TableGroupJoinProducer) pluralAttr.getElementDescriptor();
//					final TableGroupJoin tableGroupJoin = joinProducer.createTableGroupJoin(
//							navigablePath,
//							collectionTableGroup,
//							null,
//							SqlAstJoinType.LEFT,
//							true,
//							false,
//							creationState.getSqlAstCreationState()
//					);
//					return tableGroupJoin.getJoinedGroup();
//				}
//
//				// otherwise
//				return collectionTableGroup;
//			} );

			return ( (Fetchable) fkPart ).generateFetch(
					parent,
					fetchPath,
					FetchTiming.IMMEDIATE,
					true,
					null,
					creationState
			);
		}

		@Override
		public FetchBuilder cacheKeyInstance() {
			return this;
		}
	}
}
