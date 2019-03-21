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
import org.hibernate.boot.model.domain.ValueMapping;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.DependantValue;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractNonIdSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.BasicTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.ConvertibleNavigable;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.internal.domain.basic.BasicFetch;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
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

	private final BasicValueMapper<J> valueMapper;

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

		BasicValueMapping bootMapping;

		final ValueMapping valueMapping = bootAttribute.getValueMapping();
		// todo (6.0): is there a batter way to manage DependantValue?
		if ( valueMapping instanceof DependantValue ) {
			DependantValue dependantValue = (DependantValue) valueMapping;
			bootMapping = (BasicValueMapping) dependantValue.getWrappedValue();
			this.boundColumn = context.getDatabaseObjectResolver()
					.resolveColumn( dependantValue.getMappedColumns().get( 0 ) );
		}
		else {
			bootMapping = (BasicValueMapping) valueMapping;
			this.boundColumn = context.getDatabaseObjectResolver().resolveColumn( bootMapping.getMappedColumn() );
		}

		this.valueMapper = bootMapping.getResolution().getValueMapper();

		if ( valueMapper.getValueConverter() != null ) {
			log.debugf(
					"BasicValueConverter [%s] being applied for basic attribute : %s",
					valueMapper.getValueConverter(),
					getNavigableRole()
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
		return this;
	}

	@Override
	public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return valueMapper.getDomainJavaDescriptor();
	}

	@Override
	public Column getBoundColumn() {
		return boundColumn;
	}

	@Override
	public BasicValueMapper<J> getValueMapper() {
		return valueMapper;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return valueMapper.getSqlExpressableType();
	}

	@Override
	public String asLoggableText() {
		return "SingularAttributeBasic(" + getContainer().asLoggableText() + '.' + getAttributeName() + ')';
	}

	@Override
	public String toString() {
		return getNavigableRole().getFullPath() + " (basic)";
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return valueMapper.getValueConverter();
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
	@SuppressWarnings("unchecked")
	public Object resolveHydratedState(
			Object hydratedForm,
			ExecutionContext executionContext,
			SharedSessionContractImplementor session,
			Object containerInstance) {
		if ( valueMapper.getValueConverter() != null ) {
			return valueMapper.getValueConverter().toDomainValue( hydratedForm, session );
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
			SqlAstCreationState creationState) {
		return Collections.singletonList(
				qualifier.resolveColumnReference( getBoundColumn() )
		);
	}

	@Override
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		if ( clause.getInclusionChecker().test( this ) ) {
			jdbcValueCollector.collect( value, valueMapper.getSqlExpressableType(), getBoundColumn() );
		}
	}

	@Override
	public void visitJdbcTypes(
			Consumer<SqlExpressableType> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		if ( clause.getInclusionChecker().test( this ) ) {
			action.accept( getBoundColumn().getExpressableType() );
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
			DomainResultCreationState creationState) {
		return new BasicFetch( fetchParent, this, fetchTiming, creationState );
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return fetchStrategy;
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
