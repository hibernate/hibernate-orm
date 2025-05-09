/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.query.sqm.DynamicInstantiationNature;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.query.sqm.tree.jpa.AbstractJpaSelection;
import org.hibernate.type.descriptor.java.JavaType;

import org.hibernate.type.spi.TypeConfiguration;
import org.jboss.logging.Logger;

import static java.util.stream.Collectors.toList;
import static org.hibernate.query.sqm.DynamicInstantiationNature.CLASS;
import static org.hibernate.query.sqm.DynamicInstantiationNature.LIST;
import static org.hibernate.query.sqm.DynamicInstantiationNature.MAP;
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

	private static final Logger log = Logger.getLogger( SqmDynamicInstantiation.class );

	public static <R> SqmDynamicInstantiation<R> forClassInstantiation(
			JavaType<R> targetJavaType,
			NodeBuilder nodeBuilder) {
		return new SqmDynamicInstantiation<>(
				new DynamicInstantiationTargetImpl<>( CLASS, targetJavaType ),
				nodeBuilder
		);
	}

	public static <R> SqmDynamicInstantiation<R> classInstantiation(
			Class<R> targetJavaType,
			List<? extends SqmSelectableNode<?>> arguments,
			NodeBuilder nodeBuilder) {
		return new SqmDynamicInstantiation<>(
				new DynamicInstantiationTargetImpl<>( CLASS,
						nodeBuilder.getTypeConfiguration().getJavaTypeRegistry().getDescriptor( targetJavaType ) ),
				arguments,
				nodeBuilder
		);
	}

	public static <M extends Map<?, ?>> SqmDynamicInstantiation<M> forMapInstantiation(
			JavaType<M> mapJavaType,
			NodeBuilder nodeBuilder) {
		return new SqmDynamicInstantiation<>(
				new DynamicInstantiationTargetImpl<>( MAP, mapJavaType ),
				nodeBuilder
		);
	}

	public static <M extends Map<?, ?>> SqmDynamicInstantiation<M> mapInstantiation(
			List<? extends SqmSelectableNode<?>> arguments, NodeBuilder nodeBuilder) {
		return new SqmDynamicInstantiation<>(
				new DynamicInstantiationTargetImpl<>( MAP,
						nodeBuilder.getTypeConfiguration().getJavaTypeRegistry().getDescriptor( Map.class ) ),
				arguments,
				nodeBuilder
		);
	}

	public static <L extends List<?>> SqmDynamicInstantiation<L> forListInstantiation(
			JavaType<L> listJavaType,
			NodeBuilder nodeBuilder) {
		return new SqmDynamicInstantiation<>(
				new DynamicInstantiationTargetImpl<>( LIST, listJavaType ),
				nodeBuilder
		);
	}

	public static <L extends List<?>> SqmDynamicInstantiation<L> listInstantiation(
			List<? extends SqmSelectableNode<?>> arguments, NodeBuilder nodeBuilder) {
		return new SqmDynamicInstantiation<>(
				new DynamicInstantiationTargetImpl<>( LIST,
						nodeBuilder.getTypeConfiguration().getJavaTypeRegistry().getDescriptor( List.class ) ),
				arguments,
				nodeBuilder
		);
	}

	private final SqmDynamicInstantiationTarget <T> instantiationTarget;
	private List<SqmDynamicInstantiationArgument<?>> arguments;

	private SqmDynamicInstantiation(
			SqmDynamicInstantiationTarget<T> instantiationTarget,
			NodeBuilder nodeBuilder) {
		super( instantiationTarget, nodeBuilder );
		this.instantiationTarget = instantiationTarget;
	}

	private SqmDynamicInstantiation(
			SqmDynamicInstantiationTarget<T> instantiationTarget,
			List<? extends SqmSelectableNode<?>> arguments,
			NodeBuilder nodeBuilder) {
		super( instantiationTarget, nodeBuilder );
		this.instantiationTarget = instantiationTarget;
		for ( SqmSelectableNode<?> argument : arguments ) {
			final SqmDynamicInstantiationArgument<?> arg =
					new SqmDynamicInstantiationArgument<>( argument, argument.getAlias(), nodeBuilder() );
			addArgument( arg );
		}
	}

	private SqmDynamicInstantiation(
			SqmExpressible<T> sqmExpressible,
			NodeBuilder criteriaBuilder,
			SqmDynamicInstantiationTarget<T> instantiationTarget,
			List<SqmDynamicInstantiationArgument<?>> arguments) {
		super( sqmExpressible, criteriaBuilder );
		this.instantiationTarget = instantiationTarget;
		this.arguments = arguments;
	}

	public boolean checkInstantiation(TypeConfiguration typeConfiguration) {
		if ( getInstantiationTarget().getNature() == CLASS ) {
			if ( getJavaType().isArray() ) {
				// hack to accommodate the needs of jpamodelgen
				// where Class objects not available during build
				return true;
			}
			final List<Class<?>> argTypes = argumentTypes();
			if ( isFullyAliased() ) {
				final List<String> aliases =
						getArguments().stream()
								.map(SqmDynamicInstantiationArgument::getAlias)
								.collect(toList());
				return isInjectionCompatible( getJavaType(), aliases, argTypes )
					|| isConstructorCompatible( getJavaType(), argTypes, typeConfiguration );
			}
			else {
				return isConstructorCompatible( getJavaType(), argTypes, typeConfiguration );
			}
		}
		else {
			// TODO: is there anything we need to check for list/map instantiation?
			return true;
		}
	}

	private List<Class<?>> argumentTypes() {
		return getArguments().stream()
				.map( arg -> {
					final SqmExpressible<?> expressible = arg.getExpressible();
					return expressible != null && expressible.getExpressibleJavaType() != null ?
							expressible.getExpressibleJavaType().getJavaTypeClass() :
							Void.class;
				} ).collect( toList() );
	}

	public boolean isFullyAliased() {
		return getArguments().stream().allMatch( arg -> arg.getAlias() != null );
	}

	@Override
	public SqmDynamicInstantiation<T> copy(SqmCopyContext context) {
		final SqmDynamicInstantiation<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		List<SqmDynamicInstantiationArgument<?>> arguments;
		if ( this.arguments == null ) {
			arguments = null;
		}
		else {
			arguments = new ArrayList<>( this.arguments.size() );
			for ( SqmDynamicInstantiationArgument<?> argument : this.arguments ) {
				arguments.add( argument.copy( context ) );
			}
		}
		final SqmDynamicInstantiation<T> instantiation = context.registerCopy(
				this,
				new SqmDynamicInstantiation<>(
						getExpressible(),
						nodeBuilder(),
						instantiationTarget,
						arguments
				)
		);
		copyTo( instantiation, context );
		return instantiation;
	}

	public SqmDynamicInstantiationTarget<T> getInstantiationTarget() {
		return instantiationTarget;
	}

	public List<SqmDynamicInstantiationArgument<?>> getArguments() {
		return arguments;
	}

	@Override
	public JavaType<T> getJavaTypeDescriptor() {
		return getInstantiationTarget().getTargetTypeDescriptor();
	}

	@Override
	public String asLoggableText() {
		return "<new " + instantiationTarget.getJavaType().getName() + ">";
	}

	public void addArgument(SqmDynamicInstantiationArgument<?> argument) {
		if ( instantiationTarget.getNature() == LIST ) {
			// really should not have an alias...
			if ( argument.getAlias() != null && log.isDebugEnabled() ) {
				log.debugf(
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
				log.warnf(
						"Argument [%s] for dynamic Map instantiation did not declare an 'injection alias' [%s] " +
								"but such aliases are needed for dynamic Map instantiations; " +
								"will likely cause problems later translating sqm",
						argument.getSelectableNode().asLoggableText(),
						argument.getAlias()
				);
			}
		}

		if ( arguments == null ) {
			arguments = new ArrayList<>();
		}
		arguments.add( argument );
	}

	@Override
	public SqmDynamicInstantiationArgument<?> add(SqmExpression<?> expression, String alias) {
		final SqmDynamicInstantiationArgument<?> argument =
				new SqmDynamicInstantiationArgument<>( expression, alias, nodeBuilder() );
		addArgument( argument );
		return argument;
	}

	@Override
	public void add(SqmDynamicInstantiationArgument<?> aliasExpression) {
		addArgument( aliasExpression );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitDynamicInstantiation( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
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
		arguments.get( 0 ).appendHqlString( hql, context );
		for ( int i = 1; i < arguments.size(); i++ ) {
			hql.append(", ");
			arguments.get( i ).appendHqlString( hql, context );
		}

		hql.append( ')' );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmDynamicInstantiation<?> that
			&& Objects.equals( instantiationTarget, that.instantiationTarget )
			&& Objects.equals( arguments, that.arguments );
	}

	@Override
	public int hashCode() {
		return Objects.hash( instantiationTarget, arguments );
	}

	@SuppressWarnings("unused")
	public SqmDynamicInstantiation<T> makeShallowCopy() {
		return new SqmDynamicInstantiation<>( getInstantiationTarget(), nodeBuilder() );
	}

	private static class DynamicInstantiationTargetImpl<T> implements SqmDynamicInstantiationTarget<T> {
		private final DynamicInstantiationNature nature;
		private final JavaType<T> javaType;


		private DynamicInstantiationTargetImpl(DynamicInstantiationNature nature, JavaType<T> javaType) {
			this.nature = nature;
			this.javaType = javaType;
		}

		@Override
		public boolean equals(Object object) {
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

		@Override
		public Class<T> getBindableJavaType() {
			return getTargetTypeDescriptor().getJavaTypeClass();
		}

		@Override
		public SqmDomainType<T> getSqmType() {
			return null;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public void visitSubSelectableNodes(Consumer<SqmSelectableNode<?>> consumer) {
		for ( SqmDynamicInstantiationArgument<?> argument : arguments ) {
			consumer.accept( argument.getSelectableNode() );
		}
	}

	@Override
	public List<SqmSelectableNode<?>> getSelectionItems() {
		final List<SqmSelectableNode<?>> list = new ArrayList<>();
		visitSubSelectableNodes( list::add );
		return list;
	}
}
