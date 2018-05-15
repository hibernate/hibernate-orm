/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.entity;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.BasicTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierSimple;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class EntityIdentifierSimpleImpl<O,J>
		extends AbstractSingularPersistentAttribute<O,J>
		implements EntityIdentifierSimple<O,J>, BasicValuedNavigable<J> {

	private final String name;
	private final Column column;
	private final BasicType<J> basicType;
	private final IdentifierGenerator identifierGenerator;

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	public EntityIdentifierSimpleImpl(
			EntityHierarchyImpl runtimeModelHierarchy,
			RootClass bootModelRootEntity,
			RuntimeModelCreationContext creationContext) {
		super(
				runtimeModelHierarchy.getRootEntityType(),
				bootModelRootEntity.getIdentifierAttributeMapping(),
				runtimeModelHierarchy.getRootEntityType().getRepresentationStrategy().generatePropertyAccess(
						bootModelRootEntity,
						bootModelRootEntity.getIdentifierAttributeMapping(),
						runtimeModelHierarchy.getRootEntityType(),
						creationContext.getSessionFactory().getSessionFactoryOptions().getBytecodeProvider()
				),
				Disposition.ID
		);

		this.name = bootModelRootEntity.getIdentifierAttributeMapping().getName();

		final BasicValueMapping<J> basicValueMapping = (BasicValueMapping<J>) bootModelRootEntity.getIdentifierAttributeMapping().getValueMapping();
		this.column = creationContext.getDatabaseObjectResolver().resolveColumn( basicValueMapping.getMappedColumn() );
		this.basicType = basicValueMapping.resolveType();
		this.identifierGenerator = creationContext.getSessionFactory().getIdentifierGenerator( bootModelRootEntity.getEntityName() );
	}

	@Override
	public BasicTypeDescriptor<J> getNavigableType() {
		return getBasicType();
	}

	@Override
	public BasicTypeDescriptor<J> getAttributeType() {
		return (BasicTypeDescriptor<J>) super.getAttributeType();
	}



	@Override
	public boolean hasSingleIdAttribute() {
		return true;
	}

	@Override
	public List<Column> getColumns() {
		return Collections.singletonList( column );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SingularPersistentAttribute asAttribute(Class javaType) {
		return this;
	}

	@Override
	public IdentifierGenerator getIdentifierValueGenerator() {
		return identifierGenerator;
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
	public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return (BasicJavaDescriptor<J>) super.getJavaTypeDescriptor();
	}

	@Override
	public BasicTypeDescriptor<J> getType() {
		return getAttributeType();
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.BASIC;
	}

	@Override
	public String asLoggableText() {
		return "IdentifierSimple(" + getContainer().asLoggableText() + ")";
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.BASIC;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSimpleIdentifier( this );
	}

	@Override
	public DomainResult createDomainResult(
			NavigableReference navigableReference,
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		return new BasicResultImpl(
				resultVariable,
				creationState.getSqlExpressionResolver().resolveSqlSelection(
						creationState.getSqlExpressionResolver().resolveSqlExpression(
								navigableReference.getColumnReferenceQualifier(),
								column
						),
						getJavaTypeDescriptor(),
						creationContext.getSessionFactory().getTypeConfiguration()
				),
				getBoundColumn().getExpressableType()
		);
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		return createDomainResult(
				creationState.getNavigableReferenceStack().getCurrent(),
				resultVariable,
				creationState, creationContext
		);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public BasicTypeDescriptor<?> getValueGraphType() {
		return getAttributeType();
	}

	@Override
	public SimpleTypeDescriptor<?> getKeyGraphType() {
		return null;
	}

	@Override
	public boolean isOptional() {
		return false;
	}

	@Override
	public Object resolveHydratedState(
			Object hydratedForm,
			ExecutionContext executionContext,
			SharedSessionContractImplementor session,
			Object containerInstance) {
		return hydratedForm;
	}

	@Override
	public void visitJdbcTypes(
			Consumer action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		action.accept( getBoundColumn().getExpressableType() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitColumns(
			BiConsumer action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		action.accept( getBoundColumn().getExpressableType(), getBoundColumn() );
	}
}
