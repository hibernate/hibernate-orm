/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.lang.reflect.Field;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmEnumLiteral;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFieldLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.type.descriptor.java.spi.EnumJavaDescriptor;

/**
 * A delayed resolution of a non-terminal path part
 *
 * @author Steve Ebersole
 */
public class SemanticPathPartDelayedResolution implements SemanticPathPart, FullyQualifiedReflectivePathSource {
	private final FullyQualifiedReflectivePathSource parent;

	// todo (6.0) : consider reusing this PossiblePackageRoot instance, updating the state fields
	//		- we'd still add to the stack, but we'd save the instantiations

	private final String fullPath;
	private final String localName;

	@SuppressWarnings("WeakerAccess")
	public SemanticPathPartDelayedResolution(String name) {
		this( null, name );
	}

	@SuppressWarnings("WeakerAccess")
	public SemanticPathPartDelayedResolution(
			FullyQualifiedReflectivePathSource parent,
			String localName) {
		this.parent = parent;
		this.localName = localName;

		this.fullPath = parent == null ? localName : parent.append( localName ).getFullPath();
	}

	@Override
	public FullyQualifiedReflectivePathSource getParent() {
		return parent;
	}

	@Override
	public String getLocalName() {
		return localName;
	}

	@Override
	public String getFullPath() {
		return fullPath;
	}

	@Override
	public SemanticPathPartDelayedResolution append(String subPathName) {
		throw new UnsupportedOperationException( "Use #resolvePathPart instead" );
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String subName,
			boolean isTerminal,
			SqmCreationState creationState) {
		final String combinedName = this.fullPath + '.' + subName;

		final SqmCreationContext creationContext = creationState.getCreationContext();

		if ( isTerminal ) {
			final EntityDomainType entityTypeByName = creationContext.getJpaMetamodel().entity( combinedName );
			if ( entityTypeByName != null ) {
				//noinspection unchecked
				return new SqmLiteralEntityType( entityTypeByName, creationContext.getNodeBuilder() );
			}

			// the incoming subName could be a field or enum reference relative to this combinedName
			//	which would mean the combinedName must be a class name
			final ClassLoaderService classLoaderService = creationContext
					.getServiceRegistry()
					.getService( ClassLoaderService.class );

			// todo (6.0) : would be nice to leverage imported names here
			try {
				final Class referencedClass = classLoaderService.classForName( combinedName );

				if ( referencedClass.isEnum() ) {
					try {
						//noinspection unchecked
						final Enum<?> enumValue = Enum.valueOf( referencedClass, subName );
						//noinspection unchecked
						return new SqmEnumLiteral(
								enumValue,
								(EnumJavaDescriptor) creationContext.getJpaMetamodel()
										.getTypeConfiguration()
										.getJavaTypeDescriptorRegistry()
										.resolveDescriptor( referencedClass ),
								subName,
								creationContext.getNodeBuilder()
						);
					}
					catch (Exception e) {
						// ignore - it could still potentially be a static field reference
					}
				}

				try {
					final Field field = referencedClass.getDeclaredField( subName );
					//noinspection unchecked
					return new SqmFieldLiteral<>(
							field.get( null ),
							creationContext.getJpaMetamodel()
									.getTypeConfiguration()
									.getJavaTypeDescriptorRegistry()
									.resolveDescriptor( referencedClass ),
							subName,
							creationContext.getNodeBuilder()
					);
				}
				catch (Exception e) {
					// ignore - fall through to the exception below
				}
			}
			catch (ClassLoadingException e) {
				// ignore - we will hit the exception below
			}

			throw new SemanticException( "Could not resolve path terminal : " + combinedName + '.' + subName );
		}
		else {

			return new SemanticPathPartDelayedResolution( this, subName );
		}
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		return null;
	}
}
