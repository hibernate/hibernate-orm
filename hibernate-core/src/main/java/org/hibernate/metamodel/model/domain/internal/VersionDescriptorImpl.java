/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractNonIdSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.internal.ScalarQueryResultImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.ValueBinder;
import org.hibernate.type.descriptor.spi.ValueExtractor;
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


	@SuppressWarnings("unchecked")
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
						Environment.getBytecodeProvider()
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
	public QueryResult createQueryResult(
			NavigableReference navigableReference,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		return new ScalarQueryResultImpl(
				resultVariable,
				creationContext.getSqlSelectionResolver().resolveSqlSelection(
						creationContext.getSqlSelectionResolver().resolveSqlExpression(
								navigableReference.getSqlExpressionQualifier(),
								VersionDescriptorImpl.this.column
						)
				),
				this
		);
	}

	@Override
	public ValueBinder getValueBinder() {
		return getBasicType().getValueBinder();
	}

	@Override
	public ValueExtractor getValueExtractor() {
		return getBasicType().getValueExtractor();
	}
}
