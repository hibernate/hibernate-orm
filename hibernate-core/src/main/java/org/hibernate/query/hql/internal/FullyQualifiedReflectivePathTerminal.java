/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.hql.HqlInterpretationException;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmEnumLiteral;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFieldLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.criteria.Expression;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;

/**
 * @author Steve Ebersole
 */
public class FullyQualifiedReflectivePathTerminal
		extends FullyQualifiedReflectivePath
		implements SqmExpression {
	private final @Nullable SqmExpressible<?> expressibleType;
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
	public FullyQualifiedReflectivePathTerminal copy(SqmCopyContext context) {
		return this;
	}

	private Function<SemanticQueryWalker<?>, ?> resolveTerminalSemantic() {
		return semanticQueryWalker -> {
			final SqmCreationContext creationContext = creationState.getCreationContext();
			final ClassLoaderService cls =
					creationContext.getServiceRegistry().requireService( ClassLoaderService.class );
			final String fullPath = getFullPath();

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// See if it is an entity-type literal

			final EntityDomainType<?> entityDescriptor = creationContext.getJpaMetamodel().entity( fullPath );
			if ( entityDescriptor != null ) {
				return new SqmLiteralEntityType<>( entityDescriptor, creationContext.getNodeBuilder() );
			}

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// See if it is a Class FQN

			try {
				final Class<?> namedClass = cls.classForName( fullPath );
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
				final Class<?> namedClass = cls.classForName( parentFullPath );
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
					(EnumJavaType) javaTypeRegistry()
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
	public @Nullable SqmExpressible<?> getNodeType() {
		return expressibleType;
	}

	@Override
	public Object accept(SemanticQueryWalker walker) {
		return handler.apply( walker );
	}

	@Override
	public JavaType<?> getJavaTypeDescriptor() {
		return expressibleType == null ? null : expressibleType.getExpressibleJavaType();
	}


	@Override
	public void applyInferableType(@Nullable SqmExpressible type) {
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( getParent().getFullPath() );
		sb.append( '.' );
		sb.append( getLocalName() );
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
	public SqmExpression<?> as(Class type) {
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
	public JpaSelection<?> alias(String name) {
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
