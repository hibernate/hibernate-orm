package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import java.util.List;

import jakarta.annotation.Nonnull;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.sql.spi.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public class SqmElementAggregateFunction<T> extends AbstractSqmSpecificPluralPartPath<T> {
	private final String functionName;
	private final @Nullable ReturnableType<T> returnableType;

	public SqmElementAggregateFunction(@Nonnull SqmPluralValuedSimplePath<?> pluralDomainPath, @Nonnull String functionName) {
		//noinspection unchecked
		super(
				pluralDomainPath.getParentNavigablePath().append( pluralDomainPath.getNavigablePath().getLocalName(), "{" + functionName + "-element}" ),
				pluralDomainPath,
				(PluralPersistentAttribute<?, ?, ?>) pluralDomainPath.getReferencedPathSource(),
				( (SqmPluralPersistentAttribute<?, ?, T>) pluralDomainPath.getReferencedPathSource() )
						.getElementPathSource()
		);
		this.functionName = functionName;
		final var nodeBuilder = pluralDomainPath.nodeBuilder();
		final var type = switch ( functionName ) {
			case "sum" ->
					nodeBuilder.getSumReturnTypeResolver()
							.resolveFunctionReturnType(
									null,
									(SqmToSqlAstConverter) null,
									List.of( pluralDomainPath ),
									nodeBuilder.getTypeConfiguration()
							);
			case "avg" ->
					nodeBuilder.getAvgReturnTypeResolver()
							.resolveFunctionReturnType(
									null,
									(SqmToSqlAstConverter) null,
									List.of( pluralDomainPath ),
									nodeBuilder.getTypeConfiguration()
							);
			default -> null;
		};
		//noinspection unchecked
		returnableType = (ReturnableType<T>) type;
	}

	@Override
	public @Nonnull SqmBindableType<T> getExpressible() {
		return returnableType == null
				? super.getExpressible()
				: castNonNull( nodeBuilder().resolveExpressible( returnableType ) );
	}

	@Override
	public @Nonnull JavaType<T> getJavaTypeDescriptor() {
		return returnableType == null
				? super.getJavaTypeDescriptor()
				: returnableType.getExpressibleJavaType();
	}

	@Override
	public @Nonnull JavaType<T> getNodeJavaType() {
		return returnableType == null ? super.getNodeJavaType() : returnableType.getExpressibleJavaType();
	}

	@Nonnull
	@Override
	public SqmElementAggregateFunction<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final var path = context.registerCopy(
				this,
				new SqmElementAggregateFunction<T>(
						getPluralDomainPath().copy( context ),
						functionName
				)
		);
		copyTo( path, context );
		return path;
	}

	@Nonnull
	public String getFunctionName() {
		return functionName;
	}

	@Nonnull
	@Override
	public SqmPath<?> resolvePathPart(
			@Nonnull String name,
			boolean isTerminal,
			@Nonnull SqmCreationState creationState) {
		final var sqmPath = get( name, true );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitElementAggregateFunction( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( functionName ).append( "(" );
		getLhs().appendHqlString( hql, context );
		hql.append( ')' );
	}
}
