/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.internal;

import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.SqmProductionException;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityTypeLiteral;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class FullyQualifiedReflectivePathTerminal
		extends FullyQualifiedReflectivePath
		implements SqmExpression {
	private final ExpressableType expressableType;
	private final SqmCreationState creationState;

	private final Function<SemanticQueryWalker,?> handler;

	@SuppressWarnings("WeakerAccess")
	public FullyQualifiedReflectivePathTerminal(
			FullyQualifiedReflectivePathSource pathSource,
			String subPathName,
			SqmCreationState creationState) {
		super( pathSource, subPathName );
		this.creationState = creationState;

		this.handler = resolveTerminalSemantic();

		// todo (6.0) : how to calculate this?
		this.expressableType = null;
	}

	@SuppressWarnings("unchecked")
	private Function<SemanticQueryWalker, ?> resolveTerminalSemantic() {
		return semanticQueryWalker -> {
			final ClassLoaderService cls = creationState.getCreationContext().getServiceRegistry().getService( ClassLoaderService.class );
			final String fullPath = getFullPath();

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// See if it is an entity-type literal

			final EntityTypeDescriptor<Object> entityDescriptor = creationState.getCreationContext().getDomainModel().findEntityDescriptor( fullPath );
			if ( entityDescriptor != null ) {
				return new EntityTypeLiteral( entityDescriptor );
			}

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// See if it is a Class FQN

			try {
				final Class namedClass = cls.classForName( fullPath );
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
				final Class namedClass = cls.classForName( parentFullPath );
				if ( namedClass != null ) {
					if ( namedClass.isEnum() ) {
						return semanticQueryWalker.visitFullyQualifiedEnum(
								Enum.valueOf( namedClass, getLocalName() )
						);
					}
					else {
						final Field field = namedClass.getField( getLocalName() );
						return semanticQueryWalker.visitFullyQualifiedField( field );
					}
				}
			}
			catch (ClassLoadingException | NoSuchFieldException ignore) {
			}

			throw new SqmProductionException( "Unsure how to handle semantic path terminal - " + fullPath );

		};

	}

	@Override
	public ExpressableType getExpressableType() {
		return expressableType;
	}

	@Override
	public Supplier<? extends ExpressableType> getInferableType() {
		return this::getExpressableType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return (T) handler.apply( walker );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return expressableType.getJavaTypeDescriptor();
	}
}
