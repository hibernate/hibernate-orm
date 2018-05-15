/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.metamodel.model.domain.internal.entity;

import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorMappings;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class DiscriminatorDescriptorImpl<O,J> implements DiscriminatorDescriptor<J> {
	public static final String NAVIGABLE_NAME = "{discriminator}";

	private final EntityHierarchy hierarchy;
	private final BasicType<J> basicType;
	private final Column column;

	private final NavigableRole navigableRole;

	@SuppressWarnings("WeakerAccess")
	public DiscriminatorDescriptorImpl(
			EntityHierarchy hierarchy,
			BasicValueMapping<J> valueMapping,
			RuntimeModelCreationContext creationContext) {
		this.hierarchy = hierarchy;

		this.basicType = valueMapping.resolveType();
		this.column = creationContext.getDatabaseObjectResolver().resolveColumn( valueMapping.getMappedColumn() );

		this.navigableRole = hierarchy.getRootEntityType().getNavigableRole().append( NAVIGABLE_NAME );
	}

	@Override
	public ManagedTypeDescriptor<O> getContainer() {
		return hierarchy.getRootEntityType();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String getNavigableName() {
		return NAVIGABLE_NAME;
	}

	@Override
	public DiscriminatorMappings getDiscriminatorMappings() {
		// todo (6.0) : will probably need to collect these dynamically during "first phase" of runtime model creation
		throw new NotYetImplementedException(  );
	}

	@Override
	public String asLoggableText() {
		return getContainer().asLoggableText() + '.' + NAVIGABLE_NAME;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
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
	public DomainResult createDomainResult(
			NavigableReference navigableReference,
			String resultVariable,
			DomainResultCreationState creationState, DomainResultCreationContext creationContext) {
		return new BasicResultImpl(
				resultVariable,
				creationState.getSqlExpressionResolver().resolveSqlSelection(
						creationState.getSqlExpressionResolver().resolveSqlExpression(
								creationState.getColumnReferenceQualifierStack().getCurrent(),
								getBoundColumn()
						),
						getJavaTypeDescriptor(),
						creationContext.getSessionFactory().getTypeConfiguration()
				),
				getBoundColumn().getExpressableType()
		);
	}
}
