/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.model.creation.spi.DatabaseObjectResolver;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionElementEntity;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReferenceEntity;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.PluralAttributeReference;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class CollectionElementEntityImpl<J>
		extends AbstractCollectionElement<J>
		implements CollectionElementEntity<J> {

	private static final Logger log = Logger.getLogger( CollectionElementEntityImpl.class );

	private final ElementClassification elementClassification;
	private final EntityTypeDescriptor<J> entityDescriptor;

	private boolean fullyInitialized;
	private ForeignKey foreignKey;
	private Navigable foreignKeyTargetNavigable;

	public CollectionElementEntityImpl(
			PersistentCollectionDescriptor runtimeDescriptor,
			Collection bootDescriptor,
			ElementClassification elementClassification,
			RuntimeModelCreationContext creationContext) {
		super( runtimeDescriptor );
		this.elementClassification = elementClassification;

		this.entityDescriptor = resolveEntityDescriptor( elementClassification, bootDescriptor, creationContext );

		creationContext.registerNavigable( this, bootDescriptor );
	}

	private EntityTypeDescriptor<J> resolveEntityDescriptor(
			ElementClassification elementClassification,
			Collection mappingBinding,
			RuntimeModelCreationContext creationContext) {
		final String elementEntityName;
		if ( elementClassification == ElementClassification.MANY_TO_MANY ) {
			elementEntityName = ( (ToOne) mappingBinding.getElement() ).getReferencedEntityName();
		}
		else if ( elementClassification == ElementClassification.ONE_TO_MANY ) {
			elementEntityName = ( (OneToMany) mappingBinding.getElement() ).getReferencedEntityName();
		}
		else {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Unexpected collection element classification [%s] for an entity-valued element",
							elementClassification.name()
					)
			);
		}

		return creationContext.getInFlightRuntimeModel().findEntityDescriptor( elementEntityName );
	}

	@Override
	public boolean finishInitialization(Object bootReference, RuntimeModelCreationContext creationContext) {
		if ( ! fullyInitialized ) {
			try {
				final boolean done = tryFinishInitialize( (Collection) bootReference, creationContext );
				if ( ! done ) {
					return false;
				}
			}
			catch ( Exception e ) {
				log.debugf( e, "#finishInitialization threw exception : %s ", getNavigableRole().getFullPath() );
				return false;
			}

			fullyInitialized = true;
		}

		return true;
	}

	protected boolean tryFinishInitialize(
			Collection bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		final String mappedBy = bootDescriptor.getMappedByProperty();
		if ( StringHelper.isEmpty( mappedBy ) ) {
			foreignKeyTargetNavigable = getEntityDescriptor().getIdentifierDescriptor();
		}
		else {
			foreignKeyTargetNavigable = getEntityDescriptor().findNavigable( mappedBy );
		}

		assert foreignKeyTargetNavigable != null;

		// Resolve the foreign-key from the boot-model based on the entity primary table.
		// In the case of an entity that owns the many-side of a one-to-many collection that uses a join-table,
		// this guarantees we get the ForeignKey for the foreignKeyTargetNavigable.
		this.foreignKey = resolveForeignKey( bootDescriptor, getEntityDescriptor().getPrimaryTable(), creationContext );

		return true;
	}

	@Override
	public EntityTypeDescriptor<J> getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public String getEntityName() {
		return getEntityDescriptor().getEntityName();
	}

	@Override
	public String getJpaEntityName() {
		return getEntityDescriptor().getJpaEntityName();
	}

	@Override
	public ElementClassification getClassification() {
		return elementClassification;
	}

	@Override
	public Table getPrimaryDmlTable() {
		// todo (6.0) : technically this needs to be the table that holds the FK columns on the `entityDescriptor` side (secondary table, inheritance table, etc)
		return entityDescriptor.getPrimaryTable();
	}

	@Override
	public SimpleTypeDescriptor getDomainTypeDescriptor() {
		return getEntityDescriptor();
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return getEntityDescriptor().findNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		getEntityDescriptor().visitNavigables( visitor );
	}


	@Override
	public EntityJavaDescriptor<J> getJavaTypeDescriptor() {
		return getEntityDescriptor().getJavaTypeDescriptor();
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector) {
		// todo (6.0) - logic duplidated in CollectionIndexEntityImpl
		if ( joinCollector.getPrimaryTableReference() != null ) {
			final TableReference joinedTableReference = new TableReference(
					getEntityDescriptor().getPrimaryTable(),
					sqlAliasBase.generateNewAlias(),
					false
			);

			joinCollector.addSecondaryReference(
					new TableReferenceJoin(
							joinType,
							joinedTableReference,
							makePredicate(
									joinCollector.getPrimaryTableReference(),
									joinedTableReference
							)
					)
			);

			lhs = joinedTableReference;
		}
		else {
			joinCollector.addPrimaryReference(
					new TableReference(
							getEntityDescriptor().getPrimaryTable(),
							sqlAliasBase.generateNewAlias(),
							false
					)
			);

			lhs = joinCollector.getPrimaryTableReference();
		}

		getEntityDescriptor().applyTableReferenceJoins( lhs, joinType, sqlAliasBase, joinCollector );
	}

	private Predicate makePredicate(ColumnReferenceQualifier lhs, TableReference rhs) {
		final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );
		for ( ForeignKey foreignKey : getContainer().getCollectionKeyDescriptor().getJoinForeignKey().getReferringTable().getForeignKeys() ) {
			if ( foreignKey.getTargetTable().equals( rhs.getTable() ) ) {
				for( ForeignKey.ColumnMappings.ColumnMapping columnMapping : foreignKey.getColumnMappings().getColumnMappings()) {
					final ColumnReference referringColumnReference = lhs.resolveColumnReference( columnMapping.getReferringColumn() );
					final ColumnReference targetColumnReference = rhs.resolveColumnReference( columnMapping.getTargetColumn() );

					// todo (6.0) : we need some kind of validation here that the column references are properly defined

					// todo (6.0) : we could also handle this using SQL row-value syntax, e.g.:
					//		`... where ... [ (rCol1, rCol2, ...) = (tCol1, tCol2, ...) ] ...`

					conjunction.add(
							new ComparisonPredicate(
									referringColumnReference,
									ComparisonOperator.EQUAL,
									targetColumnReference
							)
					);
				}
				break;
			}
		}
		return conjunction;
	}

	@Override
	public SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationState creationState) {
		return new SqmCollectionElementReferenceEntity( (SqmPluralAttributeReference) containerReference );
	}

	@Override
	public void visitJdbcTypes(Consumer<SqlExpressableType> action, Clause clause, TypeConfiguration typeConfiguration) {
		visitColumns(
				(sqlExpressableType, column) -> action.accept( sqlExpressableType ),
				clause,
				typeConfiguration
		);
	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
		return getEntityDescriptor().getIdentifier( value, session );
	}

	@Override
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
//		getEntityDescriptor().getIdentifierDescriptor().dehydrate( value, jdbcValueCollector, clause, session );
		getEntityDescriptor().getIdentifierDescriptor().dehydrate(
				value,
				(jdbcValue, sqlExpressableType, boundColumn) ->
						jdbcValueCollector.collect(
								jdbcValue,
								sqlExpressableType,
								// todo (6.0) - this logic really belongs elsewhere, how/where to refactor?
								getClassification() == ElementClassification.ONE_TO_MANY
										? boundColumn
										: foreignKey.resolveReferringFromTargetColumn( boundColumn )
						)
				,
				clause,
				session
		);
	}

	@Override
	public void visitColumns(
			BiConsumer<SqlExpressableType, Column> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
//		if ( clause == Clause.INSERT || clause == Clause.UPDATE ) {
//			return;
//		}

		if ( getCollectionDescriptor().isOneToMany() ) {
			getEntityDescriptor().getIdentifierDescriptor().visitColumns( action, clause, typeConfiguration );
		}
		else {
			foreignKey.getColumnMappings().getReferringColumns().forEach(
					column -> action.accept( column.getExpressableType(), column )
			);
		}
	}

	@Override
	public DomainResult createDomainResult(
			NavigableReference navigableReference,
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		// delegate to the persister because here we are returning
		// 		the entities that make up the referenced collection's elements
		// we need to pass an EntityValuedNavigableReference
		EntityValuedNavigableReference entityValuedNavigableReference = new EntityValuedNavigableReference(
				navigableReference.getNavigableContainerReference(),
				getEntityDescriptor(),
				navigableReference.getNavigablePath(),
				navigableReference.getColumnReferenceQualifier(),
				( (PluralAttributeReference) navigableReference ).getLockMode()
		);
		return getEntityDescriptor().createDomainResult(
				entityValuedNavigableReference,
				resultVariable,
				creationState, creationContext
		);
	}

	@Override
	public boolean isNullable() {
		return getCollectionDescriptor().getDescribedAttribute().isNullable();
	}

	@Override
	public void visitFetchables(Consumer<Fetchable> fetchableConsumer) {
		entityDescriptor.visitFetchables( fetchableConsumer );
	}

	private ForeignKey resolveForeignKey(
			Collection bootDescriptor,
			Table table,
			RuntimeModelCreationContext creationContext) {
		final DatabaseObjectResolver databaseObjectResolver = creationContext.getDatabaseObjectResolver();
		final Iterable<ForeignKey> foreignKeys = databaseObjectResolver
				.resolveForeignKey( bootDescriptor.getForeignKey() )
				.getReferringTable()
				.getForeignKeys();

		ForeignKey resolvedForeignKey = null;

		if ( elementClassification.equals( ElementClassification.MANY_TO_MANY ) ) {
			final List<Column> elementColumns = getElementColumns( bootDescriptor, databaseObjectResolver );
			for ( ForeignKey foreignKey : foreignKeys ) {
				if ( foreignKey.getTargetTable().equals( table ) ) {
					// we can have more than one Fk targeting the same table, so we have to check if the fk referring
					// columns match with the element ones
					boolean columnsMatch = true;
					final List<Column> referringColumns = foreignKey.getColumnMappings().getReferringColumns();
					for ( Column column : elementColumns ) {
						if ( !referringColumns.contains( column ) ) {
							columnsMatch = false;
							break;
						}
					}
					if ( columnsMatch ) {
						resolvedForeignKey = foreignKey;
						break;
					}
				}
			}
		}
		else {
			for ( ForeignKey foreignKey : foreignKeys ) {
				if ( foreignKey.getReferringTable().equals( table ) ) {
					resolvedForeignKey = foreignKey;
					break;
				}
			}
		}

//		if ( resolvedForeignKey == null ) {
//			log.warnf(
//					String.format(
//							Locale.ROOT,
//							"Failed to locate foreign key for [%s] with mapped-by [%s], using fallback look-up.",
//							bootDescriptor.getRole(),
//							mappedByProperty
//					)
//			);
//
//			for ( ForeignKey foreignKey : foreignKeys ) {
//				if ( foreignKey.getReferringTable().equals( table ) ) {
//					resolvedForeignKey = foreignKey;
//					break;
//				}
//			}
//		}

		assert resolvedForeignKey != null;

		return resolvedForeignKey;
	}

	private List<Column> getElementColumns(Collection bootDescriptor, DatabaseObjectResolver databaseObjectResolver) {
		List<Column> elementColumns = new ArrayList<>();
		bootDescriptor.getElement().getMappedColumns().forEach( column -> {
			elementColumns.add( databaseObjectResolver.resolveColumn( column ) );
		} );
		return elementColumns;
	}

	@Override
	public boolean hasNotNullColumns() {
		return getEntityDescriptor().visitAndCollectStateArrayContributors( contributor -> !contributor.isNullable() )
				.stream()
				.anyMatch( value -> value == true );
	}

	@Override
	public boolean isMutable() {
		return getJavaTypeDescriptor().getMutabilityPlan().isMutable();
	}

	@Override
	public J replace(J originalValue, J targetValue, Object owner, Map copyCache, SessionImplementor session) {
		return getJavaTypeDescriptor().getMutabilityPlan().replace(
				getEntityDescriptor(),
				originalValue,
				targetValue,
				owner,
				copyCache,
				session
		);
	}

	@Override
	public boolean isDirty(Object one, Object another, SharedSessionContractImplementor session) {
		return getEntityDescriptor().isDirty( one, another, session );
	}
}
