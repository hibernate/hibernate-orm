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
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionIndex;
import org.hibernate.metamodel.model.domain.spi.CollectionIndexEntity;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionIndexReferenceEntity;
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
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class CollectionIndexEntityImpl<J>
		extends AbstractCollectionIndex<J>
		implements CollectionIndexEntity<J> {
	private final EntityTypeDescriptor<J> entityDescriptor;
	private final NavigableRole navigableRole;
	private final List<Column> columns;

	public CollectionIndexEntityImpl(
			PersistentCollectionDescriptor descriptor,
			IndexedCollection bootCollectionMapping,
			RuntimeModelCreationContext creationContext) {
		super( descriptor, bootCollectionMapping );

		this.entityDescriptor = resolveEntityDescriptor( bootCollectionMapping, creationContext );
		this.navigableRole = descriptor.getNavigableRole().append( NAVIGABLE_NAME );

		this.columns = resolveIndexColumns( bootCollectionMapping, creationContext );
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
	public SimpleTypeDescriptor<?> getDomainTypeDescriptor() {
		return getEntityDescriptor();
	}

	@Override
	public IndexClassification getClassification() {
		// todo : distinguish between OneToMany and ManyToMany
		return IndexClassification.ONE_TO_MANY;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
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
	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public EntityJavaDescriptor<J> getJavaTypeDescriptor() {
		return getEntityDescriptor().getJavaTypeDescriptor();
	}

	@Override
	public SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationState creationState) {
		return new SqmCollectionIndexReferenceEntity( (SqmPluralAttributeReference) containerReference );
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector) {
		// todo (6.0) - logic duplidated in CollectionElementEntityImpl
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
	public void visitFetchables(Consumer<Fetchable> fetchableConsumer) {
		getEntityDescriptor().visitFetchables( fetchableConsumer );
	}

	@Override
	public boolean hasNotNullColumns() {
		return getEntityDescriptor().visitAndCollectStateArrayContributors( contributor -> !contributor.isNullable() )
				.stream()
				.anyMatch( value -> value == true );
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

	private EntityTypeDescriptor<J> resolveEntityDescriptor(
			IndexedCollection collection,
			RuntimeModelCreationContext creationContext) {
		final Value indexValueMapping = collection.getIndex();

		final String indexEntityName;
		if ( indexValueMapping instanceof OneToMany ) {
			indexEntityName = ( (OneToMany) indexValueMapping ).getReferencedEntityName();
		}
		else if ( indexValueMapping instanceof ManyToOne ) {
			indexEntityName = ( (ManyToOne) indexValueMapping ).getReferencedEntityName();
		}
		else {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Failed to resolve entity descriptor for collection index [%s]",
							collection.getRole()
					)
			);
		}

		return creationContext.getInFlightRuntimeModel().findEntityDescriptor( indexEntityName );
	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
		// todo (6.0) - this does not satisfy all use cases yet.
		return ForeignKeys.getEntityIdentifierIfNotUnsaved( getEntityName(), value, session );
	}

	@Override
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		for ( Column column : columns ) {
			jdbcValueCollector.collect( value, column.getExpressableType(), column );
		}
	}

	@Override
	public void visitColumns(
			BiConsumer<SqlExpressableType, Column> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		for ( Column column : columns ) {
			action.accept( column.getExpressableType(), column );
		}
	}

	private static List<Column> resolveIndexColumns(
			IndexedCollection bootCollectionMapping,
			RuntimeModelCreationContext creationContext) {
		List<Column> columns = new ArrayList<>();
		for ( Object indexColumn : bootCollectionMapping.getIndex().getMappedColumns() ) {
			final MappedColumn mappedColumn = (MappedColumn) indexColumn;
			columns.add( creationContext.getDatabaseObjectResolver().resolveColumn( mappedColumn ) );
		}
		return columns;
	}
}
