/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.List;

import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class SqmElementAggregateFunction<T> extends AbstractSqmSpecificPluralPartPath<T> {
	private final String functionName;
	private final ReturnableType<T> returnableType;

	public SqmElementAggregateFunction(SqmPath<?> pluralDomainPath, String functionName) {
		//noinspection unchecked
		super(
				pluralDomainPath.getNavigablePath().getParent().append( pluralDomainPath.getNavigablePath().getLocalName(), "{" + functionName + "-element}" ),
				pluralDomainPath,
				(PluralPersistentAttribute<?, ?, ?>) pluralDomainPath.getReferencedPathSource(),
				( (PluralPersistentAttribute<?, ?, T>) pluralDomainPath.getReferencedPathSource() ).getElementPathSource()
		);
		this.functionName = functionName;
		switch ( functionName ) {
			case "sum":
				//noinspection unchecked
				this.returnableType = (ReturnableType<T>) nodeBuilder().getSumReturnTypeResolver()
						.resolveFunctionReturnType(
								null,
								(SqmToSqlAstConverter) null,
								List.of( pluralDomainPath ),
								nodeBuilder().getTypeConfiguration()
						);
				break;
			case "avg":
				//noinspection unchecked
				this.returnableType = (ReturnableType<T>) nodeBuilder().getAvgReturnTypeResolver()
						.resolveFunctionReturnType(
								null,
								(SqmToSqlAstConverter) null,
								List.of( pluralDomainPath ),
								nodeBuilder().getTypeConfiguration()
						);
				break;
			default:
				this.returnableType = null;
				break;
		}
	}

	@Override
	public SqmExpressible<T> getExpressible() {
		return returnableType == null ? super.getExpressible() : returnableType;
	}

	@Override
	public JavaType<T> getJavaTypeDescriptor() {
		return returnableType == null ? super.getJavaTypeDescriptor() : returnableType.getExpressibleJavaType();
	}

	@Override
	public JavaType<T> getNodeJavaType() {
		return returnableType == null ? super.getNodeJavaType() : returnableType.getExpressibleJavaType();
	}

	@Override
	public SqmElementAggregateFunction<T> copy(SqmCopyContext context) {
		final SqmElementAggregateFunction<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmElementAggregateFunction<T> path = context.registerCopy(
				this,
				new SqmElementAggregateFunction<>(
						getLhs().copy( context ),
						functionName
				)
		);
		copyTo( path, context );
		return path;
	}

	public String getFunctionName() {
		return functionName;
	}

	@Override
	public SqmPath<?> resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final SqmPath<?> sqmPath = get( name, true );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitElementAggregateFunction( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append(functionName).append( "(" );
		getLhs().appendHqlString( sb );
		sb.append( ')' );
	}
}
