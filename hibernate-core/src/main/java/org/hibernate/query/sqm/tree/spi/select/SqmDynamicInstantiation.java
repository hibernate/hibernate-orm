/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.select;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCacheable;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.domain.SqmDomainType;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;
import org.hibernate.query.sqm.tree.spi.jpa.AbstractJpaSelection;
import org.hibernate.type.descriptor.java.DateJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import org.hibernate.type.descriptor.java.TemporalJavaType;
import org.hibernate.type.spi.TypeConfiguration;
import org.jboss.logging.Logger;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.query.sqm.tree.spi.select.DynamicInstantiationNature.CLASS;
import static org.hibernate.query.sqm.tree.spi.select.DynamicInstantiationNature.LIST;
import static org.hibernate.query.sqm.tree.spi.select.DynamicInstantiationNature.MAP;
import static org.hibernate.sql.results.graph.instantiation.internal.InstantiationHelper.isConstructorCompatible;
import static org.hibernate.sql.results.graph.instantiation.internal.InstantiationHelper.isInjectionCompatible;

/**
 * Represents a dynamic instantiation ({@code select new XYZ(...) ...}) as part of the SQM.
 *
 * @author Steve Ebersole
 */
public class SqmDynamicInstantiation<T>
		extends AbstractJpaSelection<T>
		implements SqmSelectableNode<T>,
		SqmAliasedExpressionContainer<SqmDynamicInstantiationArgument<?>>,
		JpaCompoundSelection<T> {

	private static final Logger LOG = Logger.getLogger( SqmDynamicInstantiation.class );

	@Nonnull
	public static <R> SqmDynamicInstantiation<R> forClassInstantiation(
			@Nonnull JavaType<R> targetJavaType,
			@Nonnull NodeBuilder nodeBuilder) {
		return new SqmDynamicInstantiation<>(
				new DynamicInstantiationTargetImpl<>( CLASS, targetJavaType ),
				nodeBuilder
		);
	}

	@Nonnull
	public static <R> SqmDynamicInstantiation<R> classInstantiation(
			@Nonnull Class<R> targetJavaType,
			@Nonnull List<? extends SqmSelectableNode<?>> arguments,
			@Nonnull NodeBuilder nodeBuilder) {
		return new SqmDynamicInstantiation<>(
				new DynamicInstantiationTargetImpl<>( CLASS,
						nodeBuilder.getTypeConfiguration().getJavaTypeRegistry()
								.resolveDescriptor( targetJavaType ) ),
				arguments,
				nodeBuilder
		);
	}

	@Nonnull
	public static <M extends Map<?, ?>> SqmDynamicInstantiation<M> forMapInstantiation(
			@Nonnull JavaType<M> mapJavaType,
			@Nonnull NodeBuilder nodeBuilder) {
		return new SqmDynamicInstantiation<>(
				new DynamicInstantiationTargetImpl<>( MAP, mapJavaType ),
				nodeBuilder
		);
	}

	@Nonnull
	public static <M extends Map<?, ?>> SqmDynamicInstantiation<M> mapInstantiation(
			@Nonnull List<? extends SqmSelectableNode<?>> arguments, @Nonnull NodeBuilder nodeBuilder) {
		return new SqmDynamicInstantiation<>(
				new DynamicInstantiationTargetImpl<>( MAP,
						nodeBuilder.getTypeConfiguration().getJavaTypeRegistry()
								.getDescriptor( Map.class ) ),
				arguments,
				nodeBuilder
		);
	}

	@Nonnull
	public static <L extends List<?>> SqmDynamicInstantiation<L> forListInstantiation(
			@Nonnull JavaType<L> listJavaType,
			@Nonnull NodeBuilder nodeBuilder) {
		return new SqmDynamicInstantiation<>(
				new DynamicInstantiationTargetImpl<>( LIST, listJavaType ),
				nodeBuilder
		);
	}

	@Nonnull
	public static <L extends List<?>> SqmDynamicInstantiation<L> listInstantiation(
			@Nonnull List<? extends SqmSelectableNode<?>> arguments, @Nonnull NodeBuilder nodeBuilder) {
		return new SqmDynamicInstantiation<>(
				new DynamicInstantiationTargetImpl<>( LIST,
						nodeBuilder.getTypeConfiguration().getJavaTypeRegistry()
								.getDescriptor( List.class ) ),
				arguments,
				nodeBuilder
		);
	}

	private final SqmDynamicInstantiationTarget <T> instantiationTarget;
	private @Nullable List<SqmDynamicInstantiationArgument<?>> arguments;

	private SqmDynamicInstantiation(
			@Nonnull SqmDynamicInstantiationTarget<T> instantiationTarget,
			@Nonnull NodeBuilder nodeBuilder) {
		super( instantiationTarget.getSqmType(), nodeBuilder );
		this.instantiationTarget = instantiationTarget;
	}

	private SqmDynamicInstantiation(
			@Nonnull SqmDynamicInstantiationTarget<T> instantiationTarget,
			@Nonnull List<? extends SqmSelectableNode<?>> arguments,
			@Nonnull NodeBuilder nodeBuilder) {
		super( instantiationTarget.getSqmType(), nodeBuilder );
		this.instantiationTarget = instantiationTarget;
		final ArrayList<SqmDynamicInstantiationArgument<?>> newArguments = new ArrayList<>();
		for ( var argument : arguments ) {
			addArgument( instantiationTarget, newArguments, new SqmDynamicInstantiationArgument<>( argument, argument.getAlias(), nodeBuilder ) );
		}
		this.arguments = newArguments;
	}

	private SqmDynamicInstantiation(
			@Nullable SqmBindableType<T> sqmExpressible,
			@Nonnull NodeBuilder criteriaBuilder,
			@Nonnull SqmDynamicInstantiationTarget<T> instantiationTarget,
			@Nullable List<SqmDynamicInstantiationArgument<?>> arguments) {
		super( sqmExpressible, criteriaBuilder );
		this.instantiationTarget = instantiationTarget;
		this.arguments = arguments;
	}

	public boolean checkInstantiation(@Nonnull TypeConfiguration typeConfiguration) {
		if ( getInstantiationTarget().getNature() == CLASS ) {
			final Class<? extends T> javaType = castNonNull( getJavaType() );
			if ( javaType.isArray() ) {
				// hack to accommodate the needs of jpamodelgen
				// where Class objects not available during build
				return true;
			}
			final var argTypes = argumentTypes();
			if ( isFullyAliased() ) {
				if ( isConstructorCompatible( javaType, argTypes, typeConfiguration ) ) {
					return true;
				}
				final var arguments = getArguments();
				final List<String> aliases = new ArrayList<>( arguments.size() );
				for ( var argument : arguments ) {
					final String alias = argument.getAlias();
					if ( alias == null ) {
						return false;
					}
					aliases.add( alias );
				}
				return isInjectionCompatible( javaType, aliases, argTypes );
			}
			else {
				return isConstructorCompatible( javaType, argTypes, typeConfiguration );
			}
		}
		else {
			// TODO: is there anything we need to check for list/map instantiation?
			return true;
		}
	}

	@Nonnull
	private List<Class<?>> argumentTypes() {
		return getArguments().stream()
				.map( arg -> {
					final var expressible = arg.getExpressible();
					if ( expressible != null ) {
						final var expressibleJavaType = expressible.getExpressibleJavaType();
						if ( expressibleJavaType != null ) {
							return expressibleJavaType instanceof DateJavaType temporalJavaType
									// Hack to accommodate a constructor with java.sql parameter
									// types when the entity has java.util.Date as its field types.
									// (This was requested in HHH-4179 and we fixed it by accident.)
									? TemporalJavaType.resolveJavaTypeClass( temporalJavaType.getPrecision() )
									: expressibleJavaType.getJavaTypeClass();
						}
					}
					return Void.class;
				} ).collect( toList() );
	}

	public boolean isFullyAliased() {
		return getArguments().stream().allMatch( arg -> arg.getAlias() != null );
	}

	@Nonnull
	@Override
	public SqmDynamicInstantiation<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmDynamicInstantiationArgument<?>> arguments = this.arguments;
		final List<SqmDynamicInstantiationArgument<?>> newArguments;
		if ( arguments == null ) {
			newArguments = null;
		}
		else {
			newArguments = new ArrayList<>( arguments.size() );
			for ( var argument : arguments ) {
				newArguments.add( argument.copy( context ) );
			}
		}
		final SqmDynamicInstantiation<T> instantiation = context.registerCopy(
				this,
				new SqmDynamicInstantiation<>(
						getExpressible(),
						nodeBuilder(),
						instantiationTarget,
						newArguments
				)
		);
		copyTo( instantiation, context );
		return instantiation;
	}

	@Nonnull
	public SqmDynamicInstantiationTarget<T> getInstantiationTarget() {
		return instantiationTarget;
	}

	@Nonnull
	public List<SqmDynamicInstantiationArgument<?>> getArguments() {
		return arguments == null ? emptyList() : unmodifiableList( arguments );
	}

	@Nullable
	@Override
	public JavaType<T> getJavaTypeDescriptor() {
		return getInstantiationTarget().getTargetTypeDescriptor();
	}

	@Nonnull
	@Override
	public String asLoggableText() {
		return "<new " + instantiationTarget.getJavaType().getName() + ">";
	}

	public void addArgument(@Nonnull SqmDynamicInstantiationArgument<?> argument) {
		if ( arguments == null ) {
			arguments = new ArrayList<>();
		}
		addArgument( instantiationTarget, arguments, argument );
	}

	private static void addArgument(@Nonnull SqmDynamicInstantiationTarget<?> instantiationTarget, @Nonnull List<SqmDynamicInstantiationArgument<?>> arguments, @Nonnull SqmDynamicInstantiationArgument<?> argument) {
		if ( instantiationTarget.getNature() == LIST ) {
			// really should not have an alias...
			if ( argument.getAlias() != null && LOG.isDebugEnabled() ) {
				LOG.debugf(
						"Argument [%s] for dynamic List instantiation declared an 'injection alias' [%s] " +
								"but such aliases are ignored for dynamic List instantiations",
						argument.getSelectableNode().asLoggableText(),
						argument.getAlias()
				);
			}
		}
		else if ( instantiationTarget.getNature() == MAP ) {
			// must(?) have an alias...
			if ( argument.getAlias() == null ) {
				LOG.warnf(
						"Argument [%s] for dynamic Map instantiation did not declare an 'injection alias' [%s] " +
								"but such aliases are needed for dynamic Map instantiations; " +
								"will likely cause problems later translating sqm",
						argument.getSelectableNode().asLoggableText(),
						argument.getAlias()
				);
			}
		}

		arguments.add( argument );
	}

	@Nonnull
	@Override
	public SqmDynamicInstantiationArgument<?> add(@Nonnull SqmExpression<?> expression, @Nullable String alias) {
		final var argument = new SqmDynamicInstantiationArgument<>( expression, alias, nodeBuilder() );
		addArgument( argument );
		return argument;
	}

	@Override
	public void add(@Nonnull SqmDynamicInstantiationArgument<?> aliasExpression) {
		addArgument( aliasExpression );
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitDynamicInstantiation( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( "new " );
		if ( instantiationTarget.getNature() == LIST ) {
			hql.append( "list" );
		}
		else if ( instantiationTarget.getNature() == MAP ) {
			hql.append( "map" );
		}
		else {
			hql.append( instantiationTarget.getTargetTypeDescriptor().getJavaTypeClass().getTypeName() );
		}
		hql.append( '(' );
		final List<SqmDynamicInstantiationArgument<?>> arguments = castNonNull( this.arguments );
		arguments.get( 0 ).appendHqlString( hql, context );
		for ( int i = 1; i < arguments.size(); i++ ) {
			hql.append(", ");
			arguments.get( i ).appendHqlString( hql, context );
		}

		hql.append( ')' );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmDynamicInstantiation<?> that
			&& Objects.equals( instantiationTarget, that.instantiationTarget )
			&& Objects.equals( arguments, that.arguments );
	}

	@Override
	public int hashCode() {
		int result = instantiationTarget.hashCode();
		result = 31 * result + Objects.hashCode( arguments );
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmDynamicInstantiation<?> that
			&& Objects.equals( instantiationTarget, that.instantiationTarget )
			&& SqmCacheable.areCompatible( arguments, that.arguments );
	}

	@Override
	public int cacheHashCode() {
		int result = instantiationTarget.hashCode();
		result = 31 * result + SqmCacheable.cacheHashCode( arguments );
		return result;
	}

	@Nonnull
	@SuppressWarnings("unused")
	public SqmDynamicInstantiation<T> makeShallowCopy() {
		return new SqmDynamicInstantiation<>( getInstantiationTarget(), nodeBuilder() );
	}

	@Override
	public @Nullable JavaType<T> getNodeJavaType() {
		return instantiationTarget.getExpressibleJavaType();
	}

	private static class DynamicInstantiationTargetImpl<T> implements SqmDynamicInstantiationTarget<T> {
		private final DynamicInstantiationNature nature;
		private final JavaType<T> javaType;

		private DynamicInstantiationTargetImpl(DynamicInstantiationNature nature, JavaType<T> javaType) {
			this.nature = nature;
			this.javaType = javaType;
		}

		@Override
		public boolean equals(@Nullable Object object) {
			return object instanceof DynamicInstantiationTargetImpl<?> that
				&& nature == that.nature
				&& Objects.equals( javaType, that.javaType );
		}

		@Override
		public int hashCode() {
			return Objects.hash( nature, javaType );
		}

		@Override
		public DynamicInstantiationNature getNature() {
			return nature;
		}

		@Override
		public JavaType<T> getTargetTypeDescriptor() {
			return javaType;
		}

		@Override
		public JavaType<T> getExpressibleJavaType() {
			return getTargetTypeDescriptor();
		}

//		@Override
//		public Class<T> getJavaType() {
//			return getTargetTypeDescriptor().getJavaTypeClass();
//		}

		@Override
		public @Nullable SqmDomainType<T> getSqmType() {
			return null;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public void visitSubSelectableNodes(@Nonnull Consumer<SqmSelectableNode<?>> consumer) {
		for ( SqmDynamicInstantiationArgument<?> argument : getArguments() ) {
			consumer.accept( argument.getSelectableNode() );
		}
	}

	@Nonnull
	@Override
	public List<SqmSelectableNode<?>> getSelectionItems() {
		final List<SqmSelectableNode<?>> list = new ArrayList<>();
		visitSubSelectableNodes( list::add );
		return list;
	}
}
