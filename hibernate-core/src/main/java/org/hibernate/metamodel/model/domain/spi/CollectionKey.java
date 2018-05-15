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

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.internal.ForeignKeyDomainResult;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.query.sql.internal.ResolvedScalarDomainResult;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * @author Steve Ebersole
 */
public class CollectionKey implements Navigable {
	private final AbstractPersistentCollectionDescriptor collectionDescriptor;
	private final JavaTypeDescriptor javaTypeDescriptor;
	private final NavigableRole navigableRole;

	private Navigable foreignKeyTargetNavigable;
	private ForeignKey joinForeignKey;

	public CollectionKey(
			AbstractPersistentCollectionDescriptor<?, ?, ?> runtimeDescriptor,
			Collection bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		this.collectionDescriptor = runtimeDescriptor;
		this.javaTypeDescriptor = resolveJavaTypeDescriptor( bootDescriptor );
		this.navigableRole = runtimeDescriptor.getNavigableRole().append( "{collection-key}" );
		this.joinForeignKey = creationContext.getDatabaseObjectResolver().resolveForeignKey( bootDescriptor.getForeignKey() );

		final String mappedBy = bootDescriptor.getMappedByProperty();
		if ( isEmpty( mappedBy ) ) {
			this.foreignKeyTargetNavigable = runtimeDescriptor.findEntityOwnerDescriptor().getIdentifierDescriptor();
		}
		else {
			this.foreignKeyTargetNavigable = runtimeDescriptor.findEntityOwnerDescriptor().findNavigable( mappedBy );
		}
	}

	public ForeignKey getJoinForeignKey() {
		return joinForeignKey;
	}

	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	public Navigable getForeignKeyTargetNavigable() {
		return foreignKeyTargetNavigable;
	}

	/**
	 * Create a DomainResult for reading the owner/container side of the collection's FK
	 */
	public DomainResult createContainerResult(
			ColumnReferenceQualifier containerReferenceQualifier,
			SqlExpressionResolver sqlExpressionResolver) {
		return createDomainResult(
				joinForeignKey.getColumnMappings().getTargetColumns(),
				containerReferenceQualifier,
				sqlExpressionResolver
		);
	}

	/**
	 * Create a DomainResult for reading the owner/container side of the collection's FK
	 */
	public DomainResult createContainerResult(
			ColumnReferenceQualifier containerReferenceQualifier,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {

		// todo (6.0) previous instead of current?
		//		in conjunction with comment above about which columns...  which qualifier to use
		//		may be as simple as this.

		return createDomainResult(
				joinForeignKey.getColumnMappings().getTargetColumns(),
				containerReferenceQualifier,
				creationState.getSqlExpressionResolver(),
				creationState,
				creationContext
		);

	}

	private DomainResult createDomainResult(
			List<Column> columns,
			ColumnReferenceQualifier referenceQualifier,
			SqlExpressionResolver sqlExpressionResolver) {
		if ( columns.size() == 1 ) {
			return new ResolvedScalarDomainResult(
					sqlExpressionResolver.resolveSqlSelection(
							sqlExpressionResolver.resolveSqlExpression( referenceQualifier, columns.get( 0 ) ),
							columns.get( 0 ).getJavaTypeDescriptor(),
							collectionDescriptor.getSessionFactory().getTypeConfiguration()
					),
					null,
					columns.get( 0 ).getJavaTypeDescriptor()
			);
		}
		else {
			final List<SqlSelection> sqlSelections = new ArrayList<>();

			for ( Column column : columns ) {
				sqlSelections.add(
						sqlExpressionResolver.resolveSqlSelection(
								sqlExpressionResolver.resolveSqlExpression(
										referenceQualifier,
										column
								),
								column.getJavaTypeDescriptor(),
								collectionDescriptor.getSessionFactory().getTypeConfiguration()
						)
				);
			}

			return new ForeignKeyDomainResult(
					getJavaTypeDescriptor(),
					sqlSelections
			);
		}
	}

	private DomainResult createDomainResult(
			List<Column> keyColumns,
			ColumnReferenceQualifier referenceQualifier,
			SqlExpressionResolver expressionResolver,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		if ( keyColumns.size() == 1 ) {
			return new ResolvedScalarDomainResult(
					expressionResolver.resolveSqlSelection(
							expressionResolver.resolveSqlExpression( referenceQualifier, keyColumns.get( 0 ) ),
							keyColumns.get( 0 ).getJavaTypeDescriptor(),
							creationContext.getSessionFactory().getTypeConfiguration()
					),
					null,
					keyColumns.get( 0 ).getJavaTypeDescriptor()
			);
		}
		else {
			final List<SqlSelection> sqlSelections = new ArrayList<>();

			for ( Column column : keyColumns ) {
				sqlSelections.add(
						expressionResolver.resolveSqlSelection(
								expressionResolver.resolveSqlExpression(
										creationState.getColumnReferenceQualifierStack().getCurrent(),
										column
								),
								column.getJavaTypeDescriptor(),
								creationContext.getSessionFactory().getTypeConfiguration()
						)
				);
			}

			return new ForeignKeyDomainResult(
					getJavaTypeDescriptor(),
					sqlSelections
			);
		}

	}

	/**
	 * Create a DomainResult for reading the collection (elements) side of the collection's FK
	 */
	public DomainResult createCollectionResult(
			ColumnReferenceQualifier referenceQualifier,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		return createDomainResult(
				joinForeignKey.getColumnMappings().getReferringColumns(),
				referenceQualifier,
				creationState.getSqlExpressionResolver(),
				creationState,
				creationContext
		);
	}

	@Override
	public void visitColumns(
			BiConsumer action,
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
		return foreignKeyTargetNavigable.unresolve( value, session );
	}

	@Override
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		final List<Column> referringColumns = getJoinForeignKey().getColumnMappings().getReferringColumns();
		final AtomicInteger position = new AtomicInteger();

		foreignKeyTargetNavigable.dehydrate(
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
}
