/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.javassist;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.ProtectionDomain;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.bytecode.ClassFile;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.buildtime.spi.ClassFilter;
import org.hibernate.bytecode.spi.AbstractClassTransformerImpl;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * Enhance the classes allowing them to implements InterceptFieldEnabled
 * This interface is then used by Hibernate for some optimizations.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 * @author Dustin Schultz
 */
public class JavassistClassTransformer extends AbstractClassTransformerImpl {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			JavassistClassTransformer.class.getName()
	);

	/**
	 * Constructs the JavassistClassTransformer
	 *
	 * @param classFilter The filter used to determine which classes to transform
	 * @param fieldFilter The filter used to determine which fields to transform
	 */
	public JavassistClassTransformer(ClassFilter classFilter, org.hibernate.bytecode.buildtime.spi.FieldFilter fieldFilter) {
		super( classFilter, fieldFilter );
	}

	@Override
	protected byte[] doTransform(
			ClassLoader loader,
			String className,
			Class classBeingRedefined,
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer) {
		ClassFile classfile;
		try {
			// WARNING: classfile only
			classfile = new ClassFile( new DataInputStream( new ByteArrayInputStream( classfileBuffer ) ) );
		}
		catch (IOException e) {
			LOG.unableToBuildEnhancementMetamodel( className );
			return classfileBuffer;
		}

		final ClassPool cp = new ClassPool();
		cp.appendSystemPath();
		cp.appendClassPath( new ClassClassPath( this.getClass() ) );
		cp.appendClassPath( new ClassClassPath( classfile.getClass() ) );

		try {
			cp.makeClassIfNew( new ByteArrayInputStream( classfileBuffer ) );
		}
		catch (IOException e) {
			throw new RuntimeException( e.getMessage(), e );
		}

		final FieldTransformer transformer = getFieldTransformer( classfile, cp );
		if ( transformer != null ) {
			LOG.debugf( "Enhancing %s", className );

			DataOutputStream out = null;
			try {
				transformer.transform( classfile );
				final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				out = new DataOutputStream( byteStream );
				classfile.write( out );
				return byteStream.toByteArray();
			}
			catch (Exception e) {
				LOG.unableToTransformClass( e.getMessage() );
				throw new HibernateException( "Unable to transform class: " + e.getMessage() );
			}
			finally {
				try {
					if ( out != null ) {
						out.close();
					}
				}
				catch (IOException e) {
					//swallow
				}
			}
		}
		return classfileBuffer;
	}

	protected FieldTransformer getFieldTransformer(final ClassFile classfile, final ClassPool classPool) {
		if ( alreadyInstrumented( classfile ) ) {
			return null;
		}
		return new FieldTransformer(
				new FieldFilter() {
					public boolean handleRead(String desc, String name) {
						return fieldFilter.shouldInstrumentField( classfile.getName(), name );
					}

					public boolean handleWrite(String desc, String name) {
						return fieldFilter.shouldInstrumentField( classfile.getName(), name );
					}

					public boolean handleReadAccess(String fieldOwnerClassName, String fieldName) {
						return fieldFilter.shouldTransformFieldAccess( classfile.getName(), fieldOwnerClassName, fieldName );
					}

					public boolean handleWriteAccess(String fieldOwnerClassName, String fieldName) {
						return fieldFilter.shouldTransformFieldAccess( classfile.getName(), fieldOwnerClassName, fieldName );
					}
				},
				classPool
		);
	}

	private boolean alreadyInstrumented(ClassFile classfile) {
		final String[] interfaces = classfile.getInterfaces();
		for ( String anInterface : interfaces ) {
			if ( FieldHandled.class.getName().equals( anInterface ) ) {
				return true;
			}
		}
		return false;
	}
}
