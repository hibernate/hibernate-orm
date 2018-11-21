/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.entity;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractNonIdSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.internal.domain.basic.BasicFetch;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class VersionDescriptorImpl<O,J>
		extends AbstractNonIdSingularPersistentAttribute<O,J>
		implements VersionDescriptor<O,J>, BasicValuedExpressableType<J> {

	private final Column column;
	private final BasicValueMapper<J> valueMapper;
	private final VersionSupport<J> versionSupport;
	private final String unsavedValue;


	@SuppressWarnings({"unchecked", "WeakerAccess"})
	public VersionDescriptorImpl(
			EntityHierarchyImpl runtimeModelHierarchy,
			RootClass bootModelRootEntity,
			RuntimeModelCreationContext creationContext) {
		super(
				runtimeModelHierarchy.getRootEntityType(),
				bootModelRootEntity.getVersionAttributeMapping(),
				runtimeModelHierarchy.getRootEntityType().getRepresentationStrategy().generatePropertyAccess(
						bootModelRootEntity,
						bootModelRootEntity.getVersionAttributeMapping(),
						runtimeModelHierarchy.getRootEntityType(),
						creationContext.getSessionFactory().getSessionFactoryOptions().getBytecodeProvider()
				),
				Disposition.VERSION
		);

		final BasicValueMapping<J> basicValueMapping = (BasicValueMapping<J>) bootModelRootEntity.getVersionAttributeMapping().getValueMapping();

		this.column = creationContext.getDatabaseObjectResolver().resolveColumn( basicValueMapping.getMappedColumn() );
		this.valueMapper = basicValueMapping.getResolution().getValueMapper();
		this.unsavedValue =( (KeyValue) basicValueMapping ).getNullValue();

		this.versionSupport = valueMapper.getDomainJavaDescriptor().getVersionSupport();
		if ( versionSupport == null ) {
			throw new HibernateException(
					"JavaTypeDescriptor [" + valueMapper.getDomainJavaDescriptor() + "] associated with VersionDescriptor [" +
							runtimeModelHierarchy.getRootEntityType().getEntityName() +
							"] did not define VersionSupport"
			);
		}

		instantiationComplete( bootModelRootEntity.getVersionAttributeMapping(), creationContext );
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.BASIC;
	}

	@Override
	public String getUnsavedValue() {
		return unsavedValue;
	}

	@Override
	public VersionSupport getVersionSupport() {
		return versionSupport;
	}

	@Override
	public SimpleTypeDescriptor<J> getType() {
		return this;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.BASIC;
	}

	@Override
	public String asLoggableText() {
		return getContainer().asLoggableText() + '.' + getNavigableName();
	}

	@Override
	public Column getBoundColumn() {
		return column;
	}

	@Override
	public BasicValueMapper<J> getValueMapper() {
		return valueMapper;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return column.getExpressableType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return (BasicJavaDescriptor) super.getJavaTypeDescriptor();
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
								VersionDescriptorImpl.this.column
						),
						getJavaTypeDescriptor(),
						creationContext.getSessionFactory().getTypeConfiguration()
				),
				getBoundColumn().getExpressableType()
		);
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
		return null;
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
