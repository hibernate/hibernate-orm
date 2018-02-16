/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AbstractNonIdSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.JoinablePersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.ForeignKey.ColumnMappings;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceEntity;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.JoinedTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.TableGroupContext;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.sql.results.internal.EntityFetchImpl;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionGroupResolutionContext;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;


/**
 * @author Steve Ebersole
 */
public class SingularPersistentAttributeEntity<O,J>
		extends AbstractNonIdSingularPersistentAttribute<O,J>
		implements JoinablePersistentAttribute<O,J>, EntityValuedNavigable<J>, Fetchable<J>, TableGroupJoinProducer {

	private final SingularAttributeClassification classification;
	private final PersistentAttributeType persistentAttributeType;
	private final NavigableRole navigableRole;
	private final String sqlAliasStem;
	private final EntityDescriptor<J> entityDescriptor;

	private final ForeignKey foreignKey;

	public SingularPersistentAttributeEntity(
			ManagedTypeDescriptor<O> runtimeModelContainer,
			PersistentAttributeMapping bootModelAttribute,
			PropertyAccess propertyAccess,
			Disposition disposition,
			SingularAttributeClassification classification,
			RuntimeModelCreationContext context) {
		super( runtimeModelContainer, bootModelAttribute, propertyAccess, disposition );
		this.classification = classification;
		this.navigableRole = runtimeModelContainer.getNavigableRole().append( bootModelAttribute.getName() );

		final ToOne valueMapping = (ToOne) bootModelAttribute.getValueMapping();


		if ( valueMapping.getReferencedEntityName() == null ) {
			throw new MappingException(
					"Name of target entity of a to-one association not known : " + navigableRole.getFullPath()
			);
		}

		this.entityDescriptor = context.getTypeConfiguration().findEntityDescriptor( valueMapping.getReferencedEntityName() );
		if ( entityDescriptor == null ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"Cannot create SingularPersistentAttributeEntity [%s] : could not locate target entity descriptor [%s]",
							navigableRole.getFullPath(),
							valueMapping.getReferencedEntityName()
					)
			);
		}

		// todo (6.0) : we need to delay resolving this.
		//		this is essentially a "second pass".  for now we assume it
		// 		points to the target entity's PK
		assert valueMapping.isReferenceToPrimaryKey();

		this.foreignKey = context.getDatabaseObjectResolver().resolveForeignKey( valueMapping.getForeignKey() );

		if ( SingularAttributeClassification.MANY_TO_ONE.equals( classification ) ) {
			persistentAttributeType = PersistentAttributeType.MANY_TO_ONE;
		}
		else {
			persistentAttributeType = PersistentAttributeType.ONE_TO_ONE;
		}

		this.sqlAliasStem =  SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( bootModelAttribute.getName() );

		instantiationComplete( bootModelAttribute, context );
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return persistentAttributeType;
	}

	@Override
	public EntityDescriptor<J> getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public EntityValuedExpressableType<J> getType() {
		return (EntityValuedExpressableType<J>) super.getType();
	}

	@Override
	public String getJpaEntityName() {
		return entityDescriptor.getJpaEntityName();
	}

	@Override
	public EntityJavaDescriptor<J> getJavaTypeDescriptor() {
		return entityDescriptor.getJavaTypeDescriptor();
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return entityDescriptor.findNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		entityDescriptor.visitNavigables( visitor );
	}

	@Override
	public boolean isAssociation() {
		return true;
	}

	public EntityDescriptor getAssociatedEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return classification;
	}

	@Override
	public String asLoggableText() {
		return "SingularAttributeEntity([" + getAttributeTypeClassification().name() + "] " +
				getContainer().asLoggableText() + '.' + getAttributeName() +
				")";
	}

	@Override
	public String toString() {
		return asLoggableText();
	}

	public String getEntityName() {
		return entityDescriptor.getEntityName();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSingularAttributeEntity( this );
	}

	@Override
	public SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext) {
		final SqmSingularAttributeReferenceEntity reference = new SqmSingularAttributeReferenceEntity(
				containerReference,
				this,
				creationContext
		);

		return reference;
	}

	@Override
	public QueryResult createQueryResult(
			NavigableReference navigableReference,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		return entityDescriptor.createQueryResult(
				navigableReference,
				resultVariable,
				creationContext
		);
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public ManagedTypeDescriptor<J> getFetchedManagedType() {
		return entityDescriptor;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			ColumnReferenceQualifier qualifier,
			FetchStrategy fetchStrategy,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		return new EntityFetchImpl(
				fetchParent,
				qualifier,
				this,
				creationContext.getQueryOptions().getLockOptions().getEffectiveLockMode( resultVariable ),
				fetchParent.getNavigablePath().append( getNavigableName() ),
				fetchStrategy,
				creationContext
		);
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector,
			TableGroupContext tableGroupContext) {
		getEntityDescriptor().applyTableReferenceJoins( lhs, joinType, sqlAliasBase, joinCollector, tableGroupContext );
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			TableGroupInfo tableGroupInfoSource,
			JoinType joinType,
			JoinedTableGroupContext tableGroupJoinContext) {
		// todo (6.0) : something like EntityDescriptor


		final SqlAliasBase sqlAliasBase = tableGroupJoinContext.getSqlAliasBaseGenerator().createSqlAliasBase( getSqlAliasStem() );

		final TableReferenceJoinCollectorImpl joinCollector = new TableReferenceJoinCollectorImpl( tableGroupJoinContext );

		getEntityDescriptor().applyTableReferenceJoins(
				tableGroupJoinContext.getColumnReferenceQualifier(),
				tableGroupJoinContext.getTableReferenceJoinType(),
				sqlAliasBase,
				joinCollector,
				tableGroupJoinContext
		);

		return joinCollector.generateTableGroup( joinType, tableGroupInfoSource, tableGroupJoinContext );
	}

	private class TableReferenceJoinCollectorImpl implements TableReferenceJoinCollector {
		private final JoinedTableGroupContext tableGroupJoinContext;

		private TableReference rootTableReference;
		private List<TableReferenceJoin> tableReferenceJoins;
		private Predicate predicate;

		public TableReferenceJoinCollectorImpl(JoinedTableGroupContext tableGroupJoinContext) {
			this.tableGroupJoinContext = tableGroupJoinContext;
		}

		@Override
		public void addRoot(TableReference root) {
			if ( rootTableReference == null ) {
				rootTableReference = root;
			}
			else {
				collectTableReferenceJoin(
						makeJoin( tableGroupJoinContext.getLhs(), rootTableReference )
				);
			}
			predicate = makePredicate( tableGroupJoinContext.getLhs(), rootTableReference );
		}

		private TableReferenceJoin makeJoin(TableGroup lhs, TableReference rootTableReference) {
			return new TableReferenceJoin(
					JoinType.LEFT,
					rootTableReference,
					makePredicate( lhs, rootTableReference )
			);
		}

		private Predicate makePredicate(TableGroup lhs, TableReference rhs) {
			final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );

			for ( ColumnMappings.ColumnMapping columnMapping : foreignKey.getColumnMappings().getColumnMappings() ) {
				final ColumnReference referringColumnReference = lhs.resolveColumnReference(  columnMapping.getReferringColumn() );
				final ColumnReference targetColumnReference = rhs.resolveColumnReference( columnMapping.getTargetColumn() );

				// todo (6.0) : we need some kind of validation here that the column references are properly defined

				conjunction.add(
						new RelationalPredicate(
								RelationalPredicate.Operator.EQUAL,
								referringColumnReference,
								targetColumnReference
						)
				);
			}

			return conjunction;
		}

		@Override
		public void collectTableReferenceJoin(TableReferenceJoin tableReferenceJoin) {
			if ( tableReferenceJoins == null ) {
				tableReferenceJoins = new ArrayList<>();
			}
			tableReferenceJoins.add( tableReferenceJoin );
		}

		public TableGroupJoin generateTableGroup(
				JoinType joinType,
				TableGroupInfo tableGroupInfoSource,
				JoinedTableGroupContext context) {
			final EntityTableGroup joinedTableGroup = new EntityTableGroup(
					tableGroupInfoSource.getUniqueIdentifier(),
					tableGroupJoinContext.getTableSpace(),
					SingularPersistentAttributeEntity.this,
					context.getQueryOptions().getLockOptions().getEffectiveLockMode( tableGroupInfoSource.getIdentificationVariable() ),
					context.getNavigablePath(),
					rootTableReference,
					tableReferenceJoins,
					tableGroupJoinContext.getColumnReferenceQualifier()
			);
			return new TableGroupJoin( joinType, joinedTableGroup, predicate );
		}
	}

	@Override
	public List<SqlSelection> resolveSqlSelectionGroup(
			ColumnReferenceQualifier qualifier,
			SqlSelectionGroupResolutionContext resolutionContext) {
		throw new NotYetImplementedFor6Exception(  );
	}
}
