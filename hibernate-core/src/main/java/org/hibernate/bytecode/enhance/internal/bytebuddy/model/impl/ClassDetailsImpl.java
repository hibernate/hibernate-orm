/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl;

import java.util.List;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ClassDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.FieldDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.MemberDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.MethodDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelProcessingContext;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

import static org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelSourceLogging.MODEL_SOURCE_LOGGER;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * @author Steve Ebersole
 */
public class ClassDetailsImpl extends AbstractAnnotationTarget implements ClassDetails {
	private final TypeDescription typeDescription;

	private final ClassDetails superClassDetails;
	private final List<FieldDetails> fields;
	private final List<MethodDetails> methods;

	private MemberDetails identifierMember;

	public ClassDetailsImpl(String name, TypeDescription typeDescription, ModelProcessingContext processingContext) {
		super( typeDescription.getDeclaredAnnotations() );

		MODEL_SOURCE_LOGGER.debugf( "Creating ClassDetails(%s)", name );

		this.typeDescription = typeDescription;

		final ClassDetailsRegistryImpl classDetailsRegistry = ( (ModelProcessingContextImpl) processingContext ).getClassDetailsRegistry();
		// NOTE: this registration is important to avoid stack overflow problems
		classDetailsRegistry.addClassDetails( name, this );

		this.superClassDetails = interpretSuperType( typeDescription, classDetailsRegistry );

		this.fields = arrayList( typeDescription.getDeclaredFields().size() );
		for ( FieldDescription.InDefinedShape declaredField : typeDescription.getDeclaredFields() ) {
			if ( declaredField.isStatic() || declaredField.isSynthetic() || declaredField.isTransient() ) {
				continue;
			}

			final TypeDescription fieldTypeDescription = declaredField.getType().asErasure();
			final ClassDetails fieldTypeDetails = classDetailsRegistry.resolveClassDetails(
					fieldTypeDescription.getName(),
					fieldTypeDescription
			);
			final FieldDetailsImpl fieldDetails = new FieldDetailsImpl( declaredField, fieldTypeDetails );
			fields.add( fieldDetails );
			identifierMember = checkForIdentifier( fieldDetails, identifierMember, typeDescription );
		}

		this.methods = arrayList( typeDescription.getDeclaredMethods().size() );
		for ( MethodDescription.InDefinedShape declaredMethod : typeDescription.getDeclaredMethods() ) {
			if ( declaredMethod.isStatic() || declaredMethod.isSynthetic() || declaredMethod.isBridge() ) {
				continue;
			}

			// we only want to collect methods which can potentially be a getter or a setter.
			// at this point, we only look at the signatures to match either:
			//		1. non-void return with no parameter (potential getter)
			//		2. void return with single parameter (potential setter)
			final TypeDescription returnTypeDescription = declaredMethod.getReturnType().asErasure();
			final String methodName = declaredMethod.getName();

			if ( TypeDescription.VOID.equals( returnTypeDescription ) ) {
				// void return -> could be a setter
				if ( declaredMethod.getParameters().size() == 1
						&& methodName.startsWith( "set" ) ) {
					final TypeDescription methodTypeDescription = declaredMethod.getParameters().get( 0 ).getType().asErasure();
					final ClassDetails methodTypeDetails = classDetailsRegistry.resolveClassDetails(
							methodTypeDescription.getName(),
							methodTypeDescription
					);
					methods.add( new MethodDetailsImpl( declaredMethod, methodTypeDetails, MethodDetails.MethodKind.SETTER ) );
				}
			}
			else {
				// non-void return -> could be a getter
				if ( declaredMethod.getParameters().isEmpty()
						&& ( methodName.startsWith( "get" ) || methodName.startsWith( "is" ) ) ) {
					final ClassDetails methodTypeDetails = classDetailsRegistry.resolveClassDetails(
							returnTypeDescription.getName(),
							returnTypeDescription
					);
					final MethodDetailsImpl methodDetails = new MethodDetailsImpl(
							declaredMethod,
							methodTypeDetails,
							MethodDetails.MethodKind.GETTER
					);
					methods.add( methodDetails );
					identifierMember = checkForIdentifier( methodDetails, identifierMember, typeDescription );
				}
			}
		}
	}

	private static MemberDetails checkForIdentifier(MemberDetails member, MemberDetails current, TypeDescription declaringType) {
		if ( member.hasAnnotation( Id.class ) || member.hasAnnotation( EmbeddedId.class ) ) {
			if ( current != null ) {
				if ( current.getKind() != member.getKind() ) {
					throw new HibernateException(
							String.format(
									Locale.ROOT,
									"Mismatched placement of @Id/@EmbeddedId (%S) : %s, %s",
									declaringType.getActualName(),
									member.getName(),
									current.getName()
							)
					);
				}
			}
			return member;
		}

		return current;
	}

	private ClassDetails interpretSuperType(
			TypeDescription typeDescription,
			ClassDetailsRegistryImpl classDetailsRegistry) {
		final TypeDescription.Generic superClassGeneric = typeDescription.getSuperClass();
		if ( superClassGeneric == null ) {
			return null;
		}

		final TypeDescription superTypeDescription = superClassGeneric.asErasure();
		if ( TypeDescription.OBJECT.equals( superTypeDescription ) ) {
			return null;
		}

		return classDetailsRegistry.resolveClassDetails( superTypeDescription.getName(), superTypeDescription );
	}

	@Override
	public String getName() {
		return typeDescription.getName();
	}

	@Override
	public String getClassName() {
		return typeDescription.getName();
	}

	@Override
	public boolean isAbstract() {
		return typeDescription.isAbstract();
	}

	@Override
	public ClassDetails getSuperType() {
		return superClassDetails;
	}

	@Override
	public List<FieldDetails> getFields() {
		return fields;
	}

	@Override
	public List<MethodDetails> getMethods() {
		return methods;
	}

	@Override
	public MemberDetails getIdentifierMember() {
		return identifierMember;
	}

	@Override
	public String toString() {
		return "ClassDetails(" + typeDescription.getName() + ")";
	}
}
