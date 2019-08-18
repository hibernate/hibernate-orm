/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.internal.ForeignKeyDomainResult;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sql.internal.ResolvedScalarDomainResult;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmNavigableReference;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.results.internal.domain.embedded.CompositeForeignKeyResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * @author Steve Ebersole
 */
public class CollectionKey<T> implements Navigable<T> {
	private final AbstractPersistentCollectionDescriptor collectionDescriptor;
	private final JavaTypeDescriptor javaTypeDescriptor;
	private final NavigableRole navigableRole;
	private final ForeignKey joinForeignKey;

	private Navigable foreignKeyTargetNavigable;

	public CollectionKey(
			AbstractPersistentCollectionDescriptor<?, ?, ?> runtimeDescriptor,
			Collection bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		this.collectionDescriptor = runtimeDescriptor;
		this.javaTypeDescriptor = resolveJavaTypeDescriptor( bootDescriptor );
		this.navigableRole = runtimeDescriptor.getNavigableRole().append( "{collection-key}" );
		this.joinForeignKey = creationContext.getDatabaseObjectResolver().resolveForeignKey( bootDescriptor.getForeignKey() );
	}

	public ForeignKey getJoinForeignKey() {
		return joinForeignKey;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	public Navigable getForeignKeyTargetNavigable() {
		if ( foreignKeyTargetNavigable == null ) {
			createForeignKeyTargetNavigable();
		}
		return foreignKeyTargetNavigable;
	}

	/**
	 * Create a DomainResult for reading the owner/container side of the collection's FK
	 */
	public DomainResult createContainerResult(
			ColumnReferenceQualifier containerReferenceQualifier,
			DomainResultCreationState creationState) {
		assert containerReferenceQualifier != null;

		return createDomainResult(
				joinForeignKey.getColumnMappings().getTargetColumns(),
				containerReferenceQualifier,
				creationState
		);
	}


	void createForeignKeyTargetNavigable() {
		if ( !isEmpty( collectionDescriptor.getMappedByProperty() ) && collectionDescriptor.isOneToMany() ) {
			foreignKeyTargetNavigable = ( (NavigableContainer) collectionDescriptor.getElementDescriptor() ).findNavigable(
					collectionDescriptor.getMappedByProperty() );
		}
		else {
			foreignKeyTargetNavigable = collectionDescriptor.findEntityOwnerDescriptor().getIdentifierDescriptor();
		}
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath navigablePath,
			String resultVariable,
			DomainResultCreationState creationState) {
		final NavigablePath tableGroupPath = navigablePath.getParent() == null
				? navigablePath
				: navigablePath.getParent();

		return createCollectionResult(
				creationState.getFromClauseAccess().getTableGroup( tableGroupPath ),
				creationState
		);
	}

	private DomainResult createDomainResult(
			List<Column> columns,
			ColumnReferenceQualifier referenceQualifier,
			DomainResultCreationState creationState) {
		if ( columns.size() == 1 ) {
			return new ResolvedScalarDomainResult(
					resolveSqlSelection(
							referenceQualifier,
							columns.get( 0 ),
							creationState.getSqlAstCreationState()
					),
					null,
					columns.get( 0 ).getJavaTypeDescriptor()
			);
		}
		else {
			final List<SqlSelection> sqlSelections = new ArrayList<>();

			for ( Column column : columns ) {
				sqlSelections.add(
						resolveSqlSelection(
								referenceQualifier,
								column,
								creationState.getSqlAstCreationState()
						)
				);
			}

			if ( getForeignKeyTargetNavigable() instanceof EmbeddedValuedNavigable ) {
				return new CompositeForeignKeyResultImpl(
						( (EmbeddedValuedNavigable) getForeignKeyTargetNavigable() ).getEmbeddedDescriptor(),
						sqlSelections
				);
			}

			return new ForeignKeyDomainResult(
					getJavaTypeDescriptor(),
					sqlSelections
			);
		}
	}

	@Override
	public SqmNavigableReference createSqmExpression(SqmPath lhs, SqmCreationState creationState) {
		throw new UnsupportedOperationException(  );
	}

	private SqlSelection resolveSqlSelection(
			ColumnReferenceQualifier referenceQualifier,
			Column column,
			SqlAstCreationState creationState) {
		return creationState.getSqlExpressionResolver().resolveSqlSelection(
				creationState.getSqlExpressionResolver().resolveSqlExpression( referenceQualifier, column ),
				column.getJavaTypeDescriptor(),
				creationState.getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}


	/**
	 * Create a DomainResult for reading the collection (elements) side of the collection's FK
	 */
	public DomainResult createCollectionResult(
			ColumnReferenceQualifier referenceQualifier,
			DomainResultCreationState creationState) {
		return createDomainResult(
				joinForeignKey.getColumnMappings().getReferringColumns(),
				referenceQualifier,
				creationState
		);
	}

	@Override
	public void visitColumns(
			BiConsumer<SqlExpressableType, Column> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		// because of the anticipated use cases, the expectation is that we
		// should walk the columns on the collection-side of the FK
		for ( Column column : getJoinForeignKey().getColumnMappings().getReferringColumns() ) {
			action.accept( column.getExpressableType(), column );
		}
	}

	@Override
	public void visitJdbcTypes(
			Consumer<SqlExpressableType> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		for ( Column column : getJoinForeignKey().getColumnMappings().getReferringColumns() ) {
			action.accept( column.getExpressableType() );
		}
	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
		if ( getForeignKeyTargetNavigable() instanceof SingularPersistentAttributeEntity ) {
			return ( (SingularPersistentAttributeEntity) getForeignKeyTargetNavigable() ).getEntityDescriptor()
					.getIdentifierDescriptor()
					.unresolve( value, session );
		}
		return getForeignKeyTargetNavigable().unresolve( value, session );
	}

	@Override
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		final List<Column> referringColumns = getJoinForeignKey().getColumnMappings().getReferringColumns();
		final AtomicInteger position = new AtomicInteger();

		getForeignKeyTargetNavigable().dehydrate(
				value,
				(jdbcValue, type, boundColumn) -> {
					final Column fkColumn = referringColumns.get( position.getAndIncrement() );
					jdbcValueCollector.collect( jdbcValue, type, fkColumn );
				},
				clause,
				session
		);
	}

	private static JavaTypeDescriptor resolveJavaTypeDescriptor(Collection collectionValue) {
		if ( collectionValue.getJavaTypeMapping() != null ) {
			return collectionValue.getJavaTypeMapping().getJavaTypeDescriptor();
		}
		return null;
	}

	@Override
	public NavigableContainer<?> getContainer() {
		return collectionDescriptor;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitCollectionForeignKey( this );
	}

	@Override
	public String asLoggableText() {
		return "CollectionKey(" + collectionDescriptor.getNavigableRole().getFullPath() + ")";
	}

	@Override
	public PersistenceType getPersistenceType() {
		return null;
	}

	@Override
	public boolean areEqual(T x, T y) throws HibernateException {
		return getForeignKeyTargetNavigable().areEqual( x,y );
	}

	@Override
	public int extractHashCode(T o) {
		return getForeignKeyTargetNavigable().extractHashCode( o );
	}
}
