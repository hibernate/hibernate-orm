/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.TenantDiscrimination;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class TenantDiscriminationImpl implements TenantDiscrimination {
	private static final String NAVIGABLE_NAME = "{tenantId}";

	private final IdentifiableTypeDescriptor container;
	private final BasicJavaDescriptor javaDescriptor;
	private final Column column;
	private final boolean isShared;
	private final boolean useParameterBinding;

	private final NavigableRole navigableRole;

	public TenantDiscriminationImpl(
			IdentifiableTypeDescriptor container,
			BasicJavaDescriptor javaDescriptor,
			Column column,
			boolean isShared,
			boolean useParameterBinding) {
		this.container = container;
		this.javaDescriptor = javaDescriptor;
		this.column = column;
		this.isShared = isShared;
		this.useParameterBinding = useParameterBinding;

		this.navigableRole = container.getNavigableRole().append( NAVIGABLE_NAME );
	}

	@Override
	public NavigableContainer getContainer() {
		return container;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return javaDescriptor;
	}

	@Override
	public String asLoggableText() {
		return getNavigableRole().getFullPath();
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return null;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return null;
	}

	@Override
	public Class getJavaType() {
		return null;
	}

	@Override
	public Column getColumn() {
		return column;
	}

	@Override
	public boolean isShared() {
		return isShared;
	}

	@Override
	public boolean isUseParameterBinding() {
		return useParameterBinding;
	}
}
