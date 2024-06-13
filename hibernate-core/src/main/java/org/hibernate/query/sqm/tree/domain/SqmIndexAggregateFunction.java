/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.List;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class SqmIndexAggregateFunction<T> extends AbstractSqmSpecificPluralPartPath<T> {
	private final String functionName;
	private final ReturnableType<T> returnableType;

	public SqmIndexAggregateFunction(SqmPath<?> pluralDomainPath, String functionName) {
		//noinspection unchecked
		super(
				pluralDomainPath.getNavigablePath().getParent().append( pluralDomainPath.getNavigablePath().getLocalName(), "{" + functionName + "-index}" ),
				pluralDomainPath,
				(PluralPersistentAttribute<?, ?, ?>) pluralDomainPath.getReferencedPathSource(),
				(SqmPathSource<T>) ( (PluralPersistentAttribute<?, ?, ?>) pluralDomainPath.getReferencedPathSource() ).getIndexPathSource()
		);
		this.functionName = functionName;
		switch ( functionName ) {
			case "sum":
				//noinspection unchecked
				this.returnableType = (ReturnableType<T>) nodeBuilder().getSumReturnTypeResolver()
						.resolveFunctionReturnType(
								null,
								(SqmToSqlAstConverter) null,
								List.of( pluralDomainPath.get( CollectionPart.Nature.INDEX.getName() ) ),
								nodeBuilder().getTypeConfiguration()
						);
				break;
			case "avg":
				//noinspection unchecked
				this.returnableType = (ReturnableType<T>) nodeBuilder().getAvgReturnTypeResolver()
						.resolveFunctionReturnType(
								null,
								(SqmToSqlAstConverter) null,
								List.of( pluralDomainPath.get( CollectionPart.Nature.INDEX.getName() ) ),
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
	public SqmIndexAggregateFunction<T> copy(SqmCopyContext context) {
		final SqmIndexAggregateFunction<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmIndexAggregateFunction<T> path = context.registerCopy(
				this,
				new SqmIndexAggregateFunction<>(
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
		final SqmPath<?> sqmPath = get( name );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitIndexAggregateFunction( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append(functionName).append( "(" );
		getLhs().appendHqlString( sb );
		sb.append( ')' );
	}
}
