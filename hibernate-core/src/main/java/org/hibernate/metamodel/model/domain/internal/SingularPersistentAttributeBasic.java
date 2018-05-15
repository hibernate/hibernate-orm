/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractNonIdSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.BasicTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.ConvertibleNavigable;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceBasic;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.internal.domain.basic.BasicFetch;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SingularPersistentAttributeBasic<O, J>
		extends AbstractNonIdSingularPersistentAttribute<O, J>
		implements BasicValuedNavigable<J>, ConvertibleNavigable<J>, Fetchable<J> {
	private static final Logger log = Logger.getLogger( SingularPersistentAttributeBasic.class );

	private final Column boundColumn;
	private final BasicType<J> basicType;
	private final SqlExpressableType sqlExpressableType;
	private final BasicValueConverter valueConverter;
	private final FetchStrategy fetchStrategy;

	@SuppressWarnings("unchecked")
	public SingularPersistentAttributeBasic(
			ManagedTypeDescriptor<O> runtimeContainer,
			PersistentAttributeMapping bootAttribute,
			PropertyAccess propertyAccess,
			Disposition disposition,
			RuntimeModelCreationContext context) {
		super(
				runtimeContainer,
				bootAttribute,
				propertyAccess,
				disposition
		);

		final BasicValueMapping<J> basicValueMapping = (BasicValueMapping<J>) bootAttribute.getValueMapping();
		this.boundColumn = context.getDatabaseObjectResolver().resolveColumn( basicValueMapping.getMappedColumn() );
		this.basicType = basicValueMapping.resolveType();

		this.valueConverter = basicValueMapping.resolveValueConverter( context, basicType );

		if ( valueConverter != null ) {
			log.debugf(
					"BasicValueConverter [%s] being applied for basic attribute : %s",
					valueConverter,
					getNavigableRole()
			);

			sqlExpressableType = basicType.getSqlTypeDescriptor().getSqlExpressableType(
					valueConverter.getRelationalJavaDescriptor(),
					context.getSessionFactory().getTypeConfiguration()
			);
		}
		else {
			sqlExpressableType = basicType.getSqlTypeDescriptor().getSqlExpressableType(
					basicType.getJavaTypeDescriptor(),
					context.getSessionFactory().getTypeConfiguration()
			);
		}

		this.fetchStrategy = bootAttribute.isLazy()
				? new FetchStrategy( FetchTiming.DELAYED, FetchStyle.SELECT )
				: FetchStrategy.IMMEDIATE_JOIN;

		instantiationComplete( bootAttribute, context );
	}


	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.BASIC;
	}

	@Override
	public BasicTypeDescriptor<J> getType() {
		return basicType;
	}

	@Override
	public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return (BasicJavaDescriptor<J>) super.getJavaTypeDescriptor();
	}

	@Override
	public SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext) {
		return new SqmSingularAttributeReferenceBasic( containerReference, this, creationContext );
	}

	@Override
	public DomainResult createDomainResult(
			NavigableReference navigableReference,
			String resultVariable,
			DomainResultCreationState creationState, DomainResultCreationContext creationContext) {
		return new BasicResultImpl(
				resultVariable,
				creationState.getSqlExpressionResolver().resolveSqlSelection(
						creationState.getSqlExpressionResolver().resolveSqlExpression(
								navigableReference.getColumnReferenceQualifier(),
								boundColumn
						),
						getJavaTypeDescriptor(),
						creationContext.getSessionFactory().getTypeConfiguration()
				),
				getBoundColumn().getExpressableType()
		);
	}

	@Override
	public Column getBoundColumn() {
		return boundColumn;
	}

	@Override
	public String asLoggableText() {
		return "SingularAttributeBasic(" + getContainer().asLoggableText() + '.' + getAttributeName() + ')';
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return valueConverter;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.BASIC;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSingularAttributeBasic( this );
	}

	@Override
	public BasicType<J> getBasicType() {
		return basicType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object resolveHydratedState(
			Object hydratedForm,
			ExecutionContext executionContext,
			SharedSessionContractImplementor session,
			Object containerInstance) {
		if ( valueConverter != null ) {
			return valueConverter.toDomainValue( hydratedForm, session );
		}
		return hydratedForm;
	}

	@Override
	public List<Column> getColumns() {
		return Collections.singletonList( getBoundColumn() );
	}

	@Override
	public List<ColumnReference> resolveColumnReferences(
			ColumnReferenceQualifier qualifier,
			SqlAstCreationContext resolutionContext) {
		return Collections.singletonList(
				qualifier.resolveColumnReference( getBoundColumn() )
		);
	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
		if ( valueConverter != null ) {
			value = valueConverter.toRelationalValue( value, session );
		}

		return value;
	}

	@Override
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		if ( clause.getInclusionChecker().test( this ) ) {
			jdbcValueCollector.collect( value, getBoundColumn().getExpressableType(), getBoundColumn() );
		}
	}

	@Override
	public void visitJdbcTypes(
			Consumer<SqlExpressableType> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		if ( clause.getInclusionChecker().test( this ) ) {
			action.accept( sqlExpressableType );
		}
	}

	@Override
	public void visitColumns(
			BiConsumer<SqlExpressableType, Column> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		if ( clause.getInclusionChecker().test( this ) ) {
			action.accept( getBoundColumn().getExpressableType(), getBoundColumn() );
		}
	}

	@Override
	public int getNumberOfJdbcParametersNeeded() {
		return 1;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean selected, LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState, DomainResultCreationContext creationContext) {
		return new BasicFetch( fetchParent, this, fetchTiming, creationContext, creationState );
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		return new BasicResultImpl(
				resultVariable,
				creationState.getSqlExpressionResolver().resolveSqlSelection(
						creationState.getSqlExpressionResolver().resolveSqlExpression(
								creationState.getNavigableReferenceStack().getCurrent().getColumnReferenceQualifier(),
								getBoundColumn()
						),
						getJavaTypeDescriptor(),
						creationContext.getSessionFactory().getTypeConfiguration()
				),
				getBoundColumn().getExpressableType()
		);
	}

	@Override
	public SimpleTypeDescriptor<?> getValueGraphType() {
		return getAttributeType();
	}

	@Override
	public SimpleTypeDescriptor<?> getKeyGraphType() {
		return null;
	}
}
