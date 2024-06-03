/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ListPersistentAttribute;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.BasicSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.EmbeddedSqmPathSource;
import org.hibernate.query.NotIndexedCollectionException;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmQualifiedJoin;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;

import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Type;

import static java.util.Arrays.asList;

public class SqmFunctionPath<T> extends AbstractSqmPath<T> {
	private final SqmFunction<?> function;

	public SqmFunctionPath(SqmFunction<?> function) {
		this( new NavigablePath( function.toHqlString() ), function );
	}

	public SqmFunctionPath(NavigablePath navigablePath, SqmFunction<?> function) {
		super(
				navigablePath,
				determinePathSource( navigablePath, function ),
				null,
				function.nodeBuilder()
		);
		this.function = function;
	}

	private static <X> SqmPathSource<X> determinePathSource(NavigablePath navigablePath, SqmFunction<?> function) {
		//noinspection unchecked
		final SqmExpressible<X> nodeType = (SqmExpressible<X>) function.getNodeType();
		final Class<X> bindableJavaType = nodeType.getBindableJavaType();
		final ManagedType<X> managedType = function.nodeBuilder()
				.getJpaMetamodel()
				.findManagedType( bindableJavaType );
		if ( managedType == null ) {
			final BasicType<X> basicType = function.nodeBuilder().getTypeConfiguration()
					.getBasicTypeForJavaType( bindableJavaType );
			return new BasicSqmPathSource<>(
					navigablePath.getFullPath(),
					null,
					basicType,
					basicType.getRelationalJavaType(),
					Bindable.BindableType.SINGULAR_ATTRIBUTE,
					false
			);
		}
		else if ( managedType.getPersistenceType() == Type.PersistenceType.EMBEDDABLE ) {
			return new EmbeddedSqmPathSource<>(
					navigablePath.getFullPath(),
					null,
					(EmbeddableDomainType<X>) managedType,
					Bindable.BindableType.SINGULAR_ATTRIBUTE,
					false
			);
		}
		else {
			throw new IllegalArgumentException( "Unsupported return type for function: " + bindableJavaType.getName() );
		}
	}

	public SqmFunction<?> getFunction() {
		return function;
	}

	@Override
	public SqmFunctionPath<T> copy(SqmCopyContext context) {
		final SqmFunctionPath<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmFunctionPath<T> path = context.registerCopy(
				this,
				new SqmFunctionPath<>( getNavigablePath(), (SqmFunction<?>) function.copy( context ) )
		);
		copyTo( path, context );
		return path;
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
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		final SqmPathRegistry pathRegistry = creationState.getCurrentProcessingState().getPathRegistry();
		final String alias = selector.toHqlString();
		final NavigablePath navigablePath = getNavigablePath().getParent().append(
				CollectionPart.Nature.ELEMENT.getName(),
				alias
		);
		final SqmFrom<?, ?> indexedPath = pathRegistry.findFromByPath( navigablePath );
		if ( indexedPath != null ) {
			return indexedPath;
		}
		if ( !( getNodeType().getSqmPathType() instanceof BasicPluralType<?, ?> ) ) {
			throw new UnsupportedOperationException( "Index access is only supported for basic plural types." );
		}
		final QueryEngine queryEngine = creationState.getCreationContext().getQueryEngine();
		final SelfRenderingSqmFunction<?> result = queryEngine.getSqmFunctionRegistry()
				.findFunctionDescriptor( "array_get" )
				.generateSqmExpression(
						asList( function, selector ),
						null,
						queryEngine
				);
		final SqmFunctionPath<Object> path = new SqmFunctionPath<>( result );
		pathRegistry.register( path );
		return path;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitFunctionPath( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		function.appendHqlString( sb );
	}

	@Override
	public <S extends T> SqmPath<S> treatAs(Class<S> treatJavaType) {
		throw new FunctionArgumentException( "Embeddable paths cannot be TREAT-ed" );
	}

	@Override
	public <S extends T> SqmPath<S> treatAs(EntityDomainType<S> treatTarget) {
		throw new FunctionArgumentException( "Embeddable paths cannot be TREAT-ed" );
	}
}
