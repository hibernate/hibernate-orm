/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.hql.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.hql.HqlInterpretationException;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.expression.SqmEnumLiteral;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFieldLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.domain.SqmEntityDomainType;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.criteria.Expression;

import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;

/**
 * @author Steve Ebersole
 */
public class FullyQualifiedReflectivePathTerminal<E>
		extends FullyQualifiedReflectivePath
		implements SqmExpression<E> {
	private final @Nullable SqmBindableType<E> expressibleType;
	private final SqmCreationState creationState;

	private final Function<SemanticQueryWalker<?>,?> handler;

	public FullyQualifiedReflectivePathTerminal(
			FullyQualifiedReflectivePathSource pathSource,
			String subPathName,
			SqmCreationState creationState) {
		super( pathSource, subPathName );
		this.creationState = creationState;

		this.handler = resolveTerminalSemantic();

		// todo (6.0) : how to calculate this?
		this.expressibleType = null;
	}

	@Override
	public FullyQualifiedReflectivePathTerminal<E> copy(SqmCopyContext context) {
		return this;
	}

	private Function<SemanticQueryWalker<?>, ?> resolveTerminalSemantic() {
		return semanticQueryWalker -> {
			final SqmCreationContext creationContext = creationState.getCreationContext();
			final String fullPath = getFullPath();

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// See if it is an entity-type literal

			final EntityDomainType<?> entityDescriptor =
					creationContext.getJpaMetamodel().findEntityType( fullPath );
			if ( entityDescriptor != null ) {
				return new SqmLiteralEntityType<>(
						(SqmEntityDomainType<?>) entityDescriptor,
						creationContext.getNodeBuilder()
				);
			}

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// See if it is a Class FQN

			try {
				final Class<?> namedClass = creationContext.classForName( fullPath );
				if ( namedClass != null ) {
					return semanticQueryWalker.visitFullyQualifiedClass( namedClass );
				}
			}
			catch (ClassLoadingException ignore) {
			}

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Check the parent path as a Class FQN, meaning the terminal is a field or
			// 		enum-value

			final String parentFullPath = getParent().getFullPath();
			try {
				final Class<?> namedClass = creationContext.classForName( parentFullPath );
				if ( namedClass != null ) {
					return createEnumOrFieldLiteral( namedClass );
				}
			}
			catch (ClassLoadingException | NoSuchFieldException ignore) {
			}

			throw new HqlInterpretationException( "Unsure how to handle semantic path terminal - " + fullPath );
		};
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private SqmExpression createEnumOrFieldLiteral(Class namedClass) throws NoSuchFieldException {
		if ( namedClass.isEnum() ) {
			return new SqmEnumLiteral(
					Enum.valueOf( namedClass, getLocalName() ),
					(EnumJavaType)
							javaTypeRegistry()
									.resolveDescriptor( namedClass, () -> new EnumJavaType( namedClass ) ),
					getLocalName(),
					nodeBuilder()
			);
		}
		else {
			return new SqmFieldLiteral(
					namedClass.getField( getLocalName() ),
					javaTypeRegistry()
							.resolveDescriptor( namedClass, () -> new EnumJavaType( namedClass ) ),
					nodeBuilder()
			);
		}
	}

	private JavaTypeRegistry javaTypeRegistry() {
		return creationState.getCreationContext().getTypeConfiguration().getJavaTypeRegistry();
	}

	@Override
	public @Nullable SqmBindableType<E> getNodeType() {
		return expressibleType;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return (X) handler.apply( walker );
	}

	@Override
	public JavaType<E> getJavaTypeDescriptor() {
		return expressibleType == null ? null : expressibleType.getExpressibleJavaType();
	}


	@Override
	public void applyInferableType(@Nullable SqmBindableType<?> type) {
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( getParent().getFullPath() );
		hql.append( '.' );
		hql.append( getLocalName() );
	}

	@Override
	public SqmExpression<Long> asLong() {
		return null;
	}

	@Override
	public SqmExpression<Integer> asInteger() {
		return null;
	}

	@Override
	public SqmExpression<Float> asFloat() {
		return null;
	}

	@Override
	public SqmExpression<Double> asDouble() {
		return null;
	}

	@Override
	public SqmExpression<BigDecimal> asBigDecimal() {
		return null;
	}

	@Override
	public SqmExpression<BigInteger> asBigInteger() {
		return null;
	}

	@Override
	public SqmExpression<String> asString() {
		return null;
	}

	@Override
	public <X> SqmExpression<X> as(Class<X> type) {
		return null;
	}

	@Override
	public SqmPredicate isNull() {
		return null;
	}

	@Override
	public SqmPredicate isNotNull() {
		return null;
	}

	@Override
	public SqmPredicate equalTo(Expression that) {
		return null;
	}

	@Override
	public SqmPredicate equalTo(Object that) {
		return null;
	}

	@Override
	public SqmPredicate notEqualTo(Expression value) {
		return null;
	}

	@Override
	public SqmPredicate notEqualTo(Object value) {
		return null;
	}

	@Override
	public <X> SqmExpression<X> cast(Class<X> type) {
		return null;
	}

	@Override
	public SqmPredicate in(Object... values) {
		return null;
	}

	@Override
	public SqmPredicate in(Expression[] values) {
		return null;
	}

	@Override
	public SqmPredicate in(Collection values) {
		return null;
	}

	@Override
	public SqmPredicate in(Expression values) {
		return null;
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		return null;
	}

	@Override
	public JpaSelection<E> alias(String name) {
		return null;
	}

	@Override
	public boolean isCompoundSelection() {
		return false;
	}

	@Override
	public String getAlias() {
		return null;
	}

	@Override
	public NodeBuilder nodeBuilder() {
		return null;
	}
}
