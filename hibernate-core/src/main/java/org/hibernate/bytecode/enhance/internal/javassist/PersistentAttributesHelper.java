/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.javassist;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMember;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.SignatureAttribute;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * util methods to fetch attribute metadata. consistent for both field and property access types.
 *
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 * @see org.hibernate.internal.util.ReflectHelper
 */
public class PersistentAttributesHelper {

	private PersistentAttributesHelper() {
	}

	private static final CoreMessageLogger log = CoreLogging.messageLogger( PersistentAttributesHelper.class );

	public static boolean hasAnnotation(CtField attribute, Class<? extends Annotation> annotation) {
		return getAnnotation( attribute, annotation ) != null;
	}

	public static boolean hasAnnotation(CtClass ctClass, String attributeName, Class<? extends Annotation> annotation) {
		return getAnnotation( ctClass, attributeName, annotation ) != null;
	}

	public static <T extends Annotation> T getAnnotation(CtField attribute, Class<T> annotation) {
		return getAnnotation( attribute.getDeclaringClass(), attribute.getName(), annotation );
	}

	public static <T extends Annotation> T getAnnotation(CtClass ctClass, String attributeName, Class<T> annotation) {
		AccessType classAccessType = getAccessTypeOrNull( ctClass );
		CtField field = findFieldOrNull( ctClass, attributeName );
		CtMethod getter = findGetterOrNull( ctClass, attributeName );

		if ( classAccessType == AccessType.FIELD || ( field != null && getAccessTypeOrNull( field ) == AccessType.FIELD ) ) {
			return field == null ? null : getAnnotationOrNull( field, annotation );
		}
		if ( classAccessType == AccessType.PROPERTY || ( getter != null && getAccessTypeOrNull( getter ) == AccessType.PROPERTY ) ) {
			return getter == null ? null : getAnnotationOrNull( getter, annotation );
		}

		T found = ( getter == null ? null : getAnnotationOrNull( getter, annotation ) );
		if ( found == null && field != null ) {
			return getAnnotationOrNull( field, annotation );
		}
		return found;
	}

	private static <T extends Annotation> T getAnnotationOrNull(CtMember ctMember, Class<T> annotation) {
		try {
			if ( ctMember.hasAnnotation( annotation ) ) {
				return annotation.cast( ctMember.getAnnotation( annotation ) );
			}
		}
		catch (ClassNotFoundException cnfe) {
			// should never happen
		}
		return null;
	}

	private static AccessType getAccessTypeOrNull(CtMember ctMember) {
		Access access = getAnnotationOrNull( ctMember, Access.class );
		return access == null ? null : access.value();
	}

	private static AccessType getAccessTypeOrNull(CtClass ctClass) {
		try {
			if ( ctClass.hasAnnotation( Access.class ) ) {
				return ( (Access) ctClass.getAnnotation( Access.class ) ).value();
			}
			else {
				CtClass extendsClass = ctClass.getSuperclass();
				return extendsClass == null ? null : getAccessTypeOrNull( extendsClass );
			}
		}
		catch (ClassNotFoundException e) {
			return null;
		}
		catch (NotFoundException e) {
			return null;
		}
	}

	//

	/**
	 * duplicated here to take CtClass instead of Class
	 * @see org.hibernate.internal.util.ReflectHelper#locateField
	 */
	private static CtField findFieldOrNull(CtClass ctClass, String propertyName) {
		if ( ctClass == null ) {
			return null;
		}
		try {
			return ctClass.getField( propertyName );
		}
		catch ( NotFoundException nsfe ) {
			try {
				return findFieldOrNull( ctClass.getSuperclass(), propertyName );
			}
			catch (NotFoundException e) {
				return null;
			}
		}
	}

	/**
	 * duplicated here to take CtClass instead of Class
	 * @see org.hibernate.internal.util.ReflectHelper#findGetterMethod
	 */
	private static CtMethod findGetterOrNull(CtClass ctClass, String propertyName) {
		if ( ctClass == null ) {
			return null;
		}
		CtMethod method = getterOrNull( ctClass, propertyName );
		if ( method != null ) {
			return method;
		}
		try {
			// check if extends
			method = findGetterOrNull( ctClass.getSuperclass(), propertyName );
			if ( method != null ) {
				return method;
			}
			// check if implements
			for ( CtClass interfaceCtClass : ctClass.getInterfaces() ) {
				method = getterOrNull( interfaceCtClass, propertyName );
				if ( method != null ) {
					return method;
				}
			}
		}
		catch (NotFoundException nfe) {
			// give up
		}
		return null;
	}

	private static CtMethod getterOrNull(CtClass containerClass, String propertyName) {
		for ( CtMethod method : containerClass.getDeclaredMethods() ) {
			try {
				// if the method has parameters, skip it
				if ( method.isEmpty() || method.getParameterTypes().length != 0 ) {
					continue;
				}
			}
			catch (NotFoundException e) {
				continue;
			}

			final String methodName = method.getName();

			// try "get"
			if ( methodName.startsWith( "get" ) ) {
				String testStdMethod = Introspector.decapitalize( methodName.substring( 3 ) );
				String testOldMethod = methodName.substring( 3 );
				if ( testStdMethod.equals( propertyName ) || testOldMethod.equals( propertyName ) ) {
					return method;
				}
			}

			// if not "get", then try "is"
			if ( methodName.startsWith( "is" ) ) {
				String testStdMethod = Introspector.decapitalize( methodName.substring( 2 ) );
				String testOldMethod = methodName.substring( 2 );
				if ( testStdMethod.equals( propertyName ) || testOldMethod.equals( propertyName ) ) {
					return method;
				}
			}
		}
		return null;
	}

	//

	public static boolean isPossibleBiDirectionalAssociation(CtField persistentField) {
		return PersistentAttributesHelper.hasAnnotation( persistentField, OneToOne.class ) ||
				PersistentAttributesHelper.hasAnnotation( persistentField, OneToMany.class ) ||
				PersistentAttributesHelper.hasAnnotation( persistentField, ManyToOne.class ) ||
				PersistentAttributesHelper.hasAnnotation( persistentField, ManyToMany.class );
	}

	public static String getMappedBy(CtField persistentField, CtClass targetEntity, JavassistEnhancementContext context) throws NotFoundException {
		final String local = getMappedByFromAnnotation( persistentField );
		return local.isEmpty() ? getMappedByFromTargetEntity( persistentField, targetEntity, context ) : local;
	}

	private static String getMappedByFromAnnotation(CtField persistentField) {

		OneToOne oto = PersistentAttributesHelper.getAnnotation( persistentField, OneToOne.class );
		if ( oto != null ) {
			return oto.mappedBy();
		}

		OneToMany otm = PersistentAttributesHelper.getAnnotation( persistentField, OneToMany.class );
		if ( otm != null ) {
			return otm.mappedBy();
		}

		// For @ManyToOne associations, mappedBy must come from the @OneToMany side of the association

		ManyToMany mtm = PersistentAttributesHelper.getAnnotation( persistentField, ManyToMany.class );
		return mtm == null ? "" : mtm.mappedBy();
	}

	private static String getMappedByFromTargetEntity(
			CtField persistentField,
			CtClass targetEntity,
			JavassistEnhancementContext context) throws NotFoundException {
		// get mappedBy value by searching in the fields of the target entity class
		for ( CtField f : targetEntity.getDeclaredFields() ) {
			if ( context.isPersistentField( f )
					&& getMappedByFromAnnotation( f ).equals( persistentField.getName() )
					&& isAssignable( persistentField.getDeclaringClass(), inferFieldTypeName( f ) ) ) {
				log.debugf(
						"mappedBy association for field [%s#%s] is [%s#%s]",
						persistentField.getDeclaringClass().getName(),
						persistentField.getName(),
						targetEntity.getName(),
						f.getName()
				);
				return f.getName();
			}
		}
		return "";
	}

	public static CtClass getTargetEntityClass(CtClass managedCtClass, CtField persistentField) throws NotFoundException {
		// get targetEntity defined in the annotation
		try {
			OneToOne oto = PersistentAttributesHelper.getAnnotation( persistentField, OneToOne.class );
			OneToMany otm = PersistentAttributesHelper.getAnnotation( persistentField, OneToMany.class );
			ManyToOne mto = PersistentAttributesHelper.getAnnotation( persistentField, ManyToOne.class );
			ManyToMany mtm = PersistentAttributesHelper.getAnnotation( persistentField, ManyToMany.class );

			Class<?> targetClass = null;
			if ( oto != null ) {
				targetClass = oto.targetEntity();
			}
			if ( otm != null ) {
				targetClass = otm.targetEntity();
			}
			if ( mto != null ) {
				targetClass = mto.targetEntity();
			}
			if ( mtm != null ) {
				targetClass = mtm.targetEntity();
			}

			if ( targetClass != null && targetClass != void.class ) {
				return managedCtClass.getClassPool().get( targetClass.getName() );
			}
		}
		catch (NotFoundException ignore) {
		}

		// infer targetEntity from generic type signature
		String inferredTypeName = inferTypeName( managedCtClass, persistentField.getName() );
		return inferredTypeName == null ? null : managedCtClass.getClassPool().get( inferredTypeName );
	}

	/**
	 * Consistent with hasAnnotation()
	 */
	private static String inferTypeName(CtClass ctClass, String attributeName ) {
		AccessType classAccessType = getAccessTypeOrNull( ctClass );
		CtField field = findFieldOrNull( ctClass, attributeName );
		CtMethod getter = findGetterOrNull( ctClass, attributeName );

		if ( classAccessType == AccessType.FIELD || ( field != null && getAccessTypeOrNull( field ) == AccessType.FIELD ) ) {
			return field == null ? null : inferFieldTypeName( field );
		}
		if ( classAccessType == AccessType.PROPERTY || ( getter != null && getAccessTypeOrNull( getter ) == AccessType.PROPERTY ) ) {
			return getter == null ? null : inferMethodTypeName( getter );
		}

		String found = ( getter == null ? null : inferMethodTypeName( getter ) );
		if ( found == null && field != null ) {
			return inferFieldTypeName( field );
		}
		return found;
	}

	private static String inferFieldTypeName(CtField field) {
		try {
			if ( field.getFieldInfo2().getAttribute( SignatureAttribute.tag ) == null ) {
				return field.getType().getName();
			}
			return inferGenericTypeName(
					field.getType(),
					SignatureAttribute.toTypeSignature( field.getGenericSignature() )
			);
		}
		catch (BadBytecode ignore) {
			return null;
		}
		catch (NotFoundException e) {
			return null;
		}
	}

	private static String inferMethodTypeName(CtMethod method) {
		try {
			if ( method.getMethodInfo2().getAttribute( SignatureAttribute.tag ) == null ) {
				return method.getReturnType().getName();
			}
			return inferGenericTypeName(
					method.getReturnType(),
					SignatureAttribute.toMethodSignature( method.getGenericSignature() ).getReturnType()
			);
		}
		catch (BadBytecode ignore) {
			return null;
		}
		catch (NotFoundException e) {
			return null;
		}
	}

	private static String inferGenericTypeName(CtClass ctClass, SignatureAttribute.Type genericSignature) {
		// infer targetEntity from generic type signature
		if ( isAssignable( ctClass, Collection.class.getName() ) ) {
			return ( (SignatureAttribute.ClassType) genericSignature ).getTypeArguments()[0].getType().jvmTypeName();
		}
		if ( isAssignable( ctClass, Map.class.getName() ) ) {
			return ( (SignatureAttribute.ClassType) genericSignature ).getTypeArguments()[1].getType().jvmTypeName();
		}
		return ctClass.getName();
	}

	//

	public static boolean isAssignable(CtClass thisCtClass, String targetClassName) {
		if ( thisCtClass == null ) {
			return false;
		}
		if ( thisCtClass.getName().equals( targetClassName ) ) {
			return true;
		}

		try {
			// check if extends
			if ( isAssignable( thisCtClass.getSuperclass(), targetClassName ) ) {
				return true;
			}
			// check if implements
			for ( CtClass interfaceCtClass : thisCtClass.getInterfaces() ) {
				if ( isAssignable( interfaceCtClass, targetClassName ) ) {
					return true;
				}
			}
		}
		catch (NotFoundException e) {
			// keep going
		}
		return false;
	}

	public static boolean isAssignable(CtField thisCtField, String targetClassName) {
		try {
			return isAssignable( thisCtField.getType(), targetClassName );
		}
		catch (NotFoundException e) {
			// keep going
		}
		return false;
	}

}
