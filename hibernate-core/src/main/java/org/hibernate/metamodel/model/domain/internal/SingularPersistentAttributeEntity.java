/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AbstractSingularPersistentAttribute;
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
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceEntity;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.JoinedTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.TableGroupContext;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.results.internal.EntityFetchImpl;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelectionGroup;
import org.hibernate.sql.results.spi.SqlSelectionGroupResolutionContext;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;


/**
 * @author Steve Ebersole
 */
public class SingularPersistentAttributeEntity<O,J>
		extends AbstractSingularPersistentAttribute<O,J>
		implements JoinablePersistentAttribute<O,J>, EntityValuedNavigable<J>, Fetchable<J>, TableGroupJoinProducer {

	private final SingularAttributeClassification classification;
	private final EntityDescriptor<J> entityDescriptor;
	private final ColumnMappings joinColumnMappings;

	private final NavigableRole navigableRole;

	private final String sqlAliasStem;

	private final PersistentAttributeType persistentAttributeType;


	public SingularPersistentAttributeEntity(
			ManagedTypeDescriptor<O> declaringType,
			String name,
			PropertyAccess propertyAccess,
			SingularAttributeClassification classification,
			Disposition disposition,
			boolean nullable,
			ToOne valueMapping,
			RuntimeModelCreationContext context) {
		super( declaringType, name, propertyAccess, disposition, nullable, valueMapping );
		this.classification = classification;

		if ( valueMapping.getReferencedEntityName() == null ) {
			throw new MappingException(
					"Cannot create SingularPersistentAttributeEntity instance until after associated entity descriptor has been registered"
			);
		}
		this.entityDescriptor = context.getTypeConfiguration().findEntityDescriptor( valueMapping.getReferencedEntityName() );
		this.navigableRole = declaringType.getNavigableRole().append( name );

		valueMapping.createForeignKey();
		joinColumnMappings = context.getDatabaseObjectResolver().resolveColumnMappings(
				valueMapping.getConstraintColumns(),
				valueMapping.getMappedColumns()
		);

		if ( valueMapping instanceof ManyToOne ) {
			persistentAttributeType = PersistentAttributeType.MANY_TO_ONE;
		}
		else {
			persistentAttributeType = PersistentAttributeType.ONE_TO_ONE;
		}

		this.sqlAliasStem =  SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( name );
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
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return entityDescriptor.findDeclaredNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		entityDescriptor.visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
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
			SqmReferenceCreationContext creationContext) {
		return new SqmSingularAttributeReferenceEntity(
				containerReference,
				this
		);
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
		throw new NotYetImplementedException(  );
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
				fetchParent.getNavigablePath().append( getNavigableName() ),
				fetchStrategy,
				creationContext
		);
	}

	protected TableReference resolveJoinTableReference(SqlAliasBase sqlAliasBase) {
		// todo (6.0) : @JoinTable handling
		return null;
	}

	protected ForeignKey getForeignKey() {
		// todo (6.0) : ForeignKey handling
		return null;
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
		throw new NotYetImplementedException(  );
//		final TableReference joinTableReference = resolveJoinTableReference( sqlAliasBase );
//		if ( joinTableReference == null ) {
//			getEntityDescriptor().applyTableReferenceJoins(
//					lhs,
//					joinType,
//					sqlAliasBase,
//					joinCollector
//			);
//		}
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			TableGroupInfo tableGroupInfoSource,
			JoinType joinType,
			JoinedTableGroupContext tableGroupJoinContext) {
		throw new NotYetImplementedException(  );
//		final SqlAliasBase sqlAliasBase = tableGroupJoinContext.getSqlAliasBaseGenerator().createSqlAliasBase( getSqlAliasStem() );
//
//		final TableReferenceJoinCollectorImpl joinCollector = new TableReferenceJoinCollectorImpl( tableGroupJoinContext );
//
//		getEntityDescriptor().applyTableReferenceJoins(
//				joinType,
//				sqlAliasBase,
//				joinCollector,
//				tableGroupJoinContext
//
//		);
//
//		return joinCollector.generateTableGroup( joinType, tableGroupInfoSource, joinedReference );
	}

//	private class TableReferenceJoinCollectorImpl implements TableReferenceJoinCollector {
//		private final JoinedTableGroupContext tableGroupJoinContext;
//
//		private TableReference rootTableReference = getJoinTableReference();
//		private List<TableReferenceJoin> tableReferenceJoins;
//		private Predicate predicate;
//
//		public TableReferenceJoinCollectorImpl(JoinedTableGroupContext tableGroupJoinContext) {
//			this.tableGroupJoinContext = tableGroupJoinContext;
//		}
//
//		@Override
//		public void addRoot(TableReference root) {
//			if ( rootTableReference == null ) {
//				rootTableReference = root;
//			}
//			else {
//				collectTableReferenceJoin(
//						makeJoin( tableGroupJoinContext.getLhs(), rootTableReference, getForeignKey() )
//				);
//			}
//			predicate = makePredicate( tableGroupJoinContext.getLhs(), rootTableReference );
//		}
//
//		private TableReferenceJoin makeJoin(TableGroup lhs, TableReference rootTableReference, ForeignKey foreignKey) {
//			return new TableReferenceJoin(
//					JoinType.LEFT,
//					rootTableReference,
//					makePredicate( lhs, rootTableReference )
//			);
//		}
//
//		private Predicate makePredicate(TableGroup lhs, TableReference rhs) {
//			final ForeignKey fk = getForeignKey();
//			final ForeignKey.ColumnMappings joinPredicateColumnMappings = fk.getColumnMappings();
//
//			final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );
//
//			for ( ForeignKey.ColumnMapping columnMapping : joinPredicateColumnMappings.getColumnMappings() ) {
//				conjunction.add(
//						new RelationalPredicate(
//								RelationalPredicate.Operator.EQUAL,
//								lhs.resolveColumnReference( columnMapping.getTargetColumn() ),
//								rhs.resolveColumnReference( columnMapping.getReferringColumn() )
//						)
//				);
//			}
//
//			return conjunction;
//		}
//
//		@Override
//		public void collectTableReferenceJoin(TableReferenceJoin tableReferenceJoin) {
//			if ( tableReferenceJoins == null ) {
//				tableReferenceJoins = new ArrayList<>();
//			}
//			tableReferenceJoins.add( tableReferenceJoin );
//		}
//
//		public TableGroupJoin generateTableGroup(
//				JoinType joinType,
//				TableGroupInfoSource tableGroupInfoSource,
//				NavigableReference joinedReference) {
//			final NavigableContainerReference navigableContainerReference = (NavigableContainerReference) joinedReference;
//			final EntityValuedNavigable entityValuedNavigable = (EntityValuedNavigable) navigableContainerReference.getNavigable();
//			final EntityTableGroup joinedTableGroup = new EntityTableGroup(
//					tableGroupInfoSource.getUniqueIdentifier(),
//					tableGroupJoinContext.getTableSpace(),
//					entityValuedNavigable.getEntityDescriptor(),
//					(EntityValuedExpressableType) joinedReference.getNavigable(),
//					joinedReference.getNavigablePath(),
//					rootTableReference,
//					tableReferenceJoins
//			);
//			return new TableGroupJoin( joinType, joinedTableGroup, predicate );
//		}
//	}

	@Override
	public ColumnMappings getJoinColumnMappings() {
		return joinColumnMappings;
	}

	@Override
	public List<Navigable> getNavigables() {
		return entityDescriptor.getNavigables();
	}

	@Override
	public List<Navigable> getDeclaredNavigables() {
		return entityDescriptor.getDeclaredNavigables();
	}

	@Override
	public SqlSelectionGroup resolveSqlSelectionGroup(
			ColumnReferenceQualifier qualifier,
			SqlSelectionGroupResolutionContext resolutionContext) {
		throw new NotYetImplementedException(  );
	}
}
