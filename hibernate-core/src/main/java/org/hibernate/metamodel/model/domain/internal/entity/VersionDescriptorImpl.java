/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.entity;

import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractNonIdSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;
import org.hibernate.metamodel.model.relational.spi.Column;
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
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class VersionDescriptorImpl<O,J>
		extends AbstractNonIdSingularPersistentAttribute<O,J>
		implements VersionDescriptor<O,J>, BasicValuedExpressableType<J> {
	private final BasicType<J> basicType;
	private final VersionSupport<J> versionSupport;

	private final Column column;
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
		this.unsavedValue =( (KeyValue) basicValueMapping ).getNullValue();

		this.basicType = basicValueMapping.resolveType();

		final Optional<VersionSupport<J>> versionSupportOptional = getBasicType().getVersionSupport();
		if ( ! versionSupportOptional.isPresent() ) {
			throw new HibernateException(
					"BasicType [" + basicType + "] associated with VersionDescriptor [" +
							runtimeModelHierarchy.getRootEntityType().getEntityName() +
							"] did not define VersionSupport"
			);
		}
		else {
			versionSupport = versionSupportOptional.get();
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
	public BasicType<J> getBasicType() {
		return basicType;
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
