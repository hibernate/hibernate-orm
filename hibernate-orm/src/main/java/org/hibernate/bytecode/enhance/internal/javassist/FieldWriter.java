/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.javassist;

import javax.persistence.Transient;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.annotation.Annotation;

import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class FieldWriter {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( FieldWriter.class );

	private FieldWriter() { }

	/* --- */

	/**
	 * Add enhancement field
	 */
	public static void addField(CtClass target, CtClass type, String field) {
		addPrivateTransient( target, type, field );
	}

	/**
	 * Add enhancement field with getter and setter
	 */
	public static void addFieldWithGetterAndSetter(CtClass target, CtClass type, String field, String getter, String setter) {
		addPrivateTransient( target, type, field );
		MethodWriter.addGetter( target, field, getter );
		MethodWriter.addSetter( target, field, setter );
	}

	/* --- */

	private static void addPrivateTransient(CtClass target, CtClass type, String name) {
		addWithModifiers( target, type, name, Modifier.PRIVATE | Modifier.TRANSIENT, Transient.class );
		log.debugf( "Wrote field into [%s]: @Transient private transient %s %s;", target.getName(), type.getName(), name );
	}

	private static void addWithModifiers(CtClass target, CtClass type, String  name, int modifiers, Class<?> ... annotations ) {
		try {
			final CtField f = new CtField( type, name, target );
			f.setModifiers( f.getModifiers() | modifiers );
			addAnnotations( f.getFieldInfo(), annotations );
			target.addField( f );
		}
		catch (CannotCompileException cce) {
			final String msg = String.format( "Could not enhance class [%s] to add field [%s]", target.getName(), name );
			throw new EnhancementException( msg, cce );
		}
	}

	/* --- */

	private static void addAnnotations(FieldInfo fieldInfo, Class<?>[] annotations) {
		AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute) fieldInfo.getAttribute( AnnotationsAttribute.visibleTag );
		if ( annotationsAttribute == null ) {
			annotationsAttribute = new AnnotationsAttribute( fieldInfo.getConstPool(), AnnotationsAttribute.visibleTag );
			fieldInfo.addAttribute( annotationsAttribute );
		}
		for (Class<?> annotation : annotations) {
			annotationsAttribute.addAnnotation( new Annotation( annotation.getName(), fieldInfo.getConstPool() ) );
		}
	}

}
