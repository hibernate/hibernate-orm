/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.javassist;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * utility class to compile methods and add the to class files
 *
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class MethodWriter {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( MethodWriter.class );

	private MethodWriter() { }

	/* --- */

	/**
	 * convenience method that builds a method from a format string. {@see String.format} for more details
	 *
	 * @throws CannotCompileException
	 */
	public static CtMethod write(CtClass target, String format, Object ... args) throws CannotCompileException {
		String body = String.format( format, args );
		// System.out.printf( "writing method into [%s]:%n%s%n", target.getName(), body );
		log.debugf( "writing method into [%s]:%n%s", target.getName(), body );
		CtMethod method = CtNewMethod.make( body, target );
		target.addMethod( method );
		return method;
	}

	/* --- */

	public static CtMethod addGetter(CtClass target, String field, String name) {
		CtField actualField = null;
		try {
			actualField = target.getField( field );
			log.debugf( "Writing getter method [%s] into [%s] for field [%s]", name, target.getName(), field );
			CtMethod method = CtNewMethod.getter( name, target.getField( field ) );
			target.addMethod( method );
			return method;
		}
		catch (CannotCompileException cce) {
			try {
				// Fall back to create a getter from delegation.
				CtMethod method = CtNewMethod.delegator( CtNewMethod.getter( name, actualField ), target );
				target.addMethod( method );
				return method;
			}
			catch (CannotCompileException ignored) {
				String msg = String.format( "Could not enhance class [%s] to add method [%s] for field [%s]", target.getName(), name, field );
				throw new EnhancementException( msg, cce );
			}
		}
		catch (NotFoundException nfe) {
			String msg = String.format( "Could not enhance class [%s] to add method [%s] for field [%s]", target.getName(), name, field );
			throw new EnhancementException( msg, nfe );
		}
	}

	public static CtMethod addSetter(CtClass target, String field, String name) {
		CtField actualField = null;
		try {
			actualField = target.getField( field );
			log.debugf( "Writing setter method [%s] into [%s] for field [%s]", name, target.getName(), field );
			CtMethod method = CtNewMethod.setter( name, actualField );
			target.addMethod( method );
			return method;
		}
		catch (CannotCompileException cce) {
			try {
				// Fall back to create a getter from delegation.
				CtMethod method = CtNewMethod.delegator( CtNewMethod.setter( name, actualField ), target );
				target.addMethod( method );
				return method;
			}
			catch (CannotCompileException ignored) {
				String msg = String.format( "Could not enhance class [%s] to add method [%s] for field [%s]", target.getName(), name, field );
				throw new EnhancementException( msg, cce );
			}
		}
		catch (NotFoundException nfe) {
			String msg = String.format( "Could not enhance class [%s] to add method [%s] for field [%s]", target.getName(), name, field );
			throw new EnhancementException( msg, nfe );
		}
	}

	/* --- */

	public static int addMethod(ConstPool cPool, CtMethod method) {
		return cPool.addMethodrefInfo( cPool.getThisClassInfo(), method.getName(), method.getSignature() );
	}

}
