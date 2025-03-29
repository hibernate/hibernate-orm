/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmSimplePath<T> extends AbstractSqmPath<T> implements SqmSimplePath<T> {

	public AbstractSqmSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, referencedPathSource, lhs, null, nodeBuilder );
	}

	public AbstractSqmSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );

		setExplicitAlias( explicitAlias );
	}

	@Override
	public void appendHqlString(StringBuilder hql) {
		if ( getLhs() != null ) {
			getLhs().appendHqlString( hql );
			hql.append( '.' );
		}
		hql.append( getReferencedPathSource().getPathName() );
	}

	@Override
	public SqmPathSource<T> getNodeType() {
		return getReferencedPathSource();
	}

	@Override
	public SqmPathSource<T> getReferencedPathSource() {
		final SqmPathSource<T> pathSource = super.getNodeType();
		if ( pathSource instanceof SingularPersistentAttribute ) {
			return ( (SingularPersistentAttribute<?, T>) pathSource ).getPathSource();
		}
		return pathSource;
	}

	@Override
	public SqmPathSource<T> getModel() {
		return super.getNodeType();
	}
}
