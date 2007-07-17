//$Id: $
package org.hibernate.bytecode.javassist;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.ProtectionDomain;

import javassist.bytecode.ClassFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.AbstractClassTransformerImpl;
import org.hibernate.bytecode.util.ClassFilter;

/**
 * Enhance the classes allowing them to implements InterceptFieldEnabled
 * This interface is then used by Hibernate for some optimizations.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class JavassistClassTransformer extends AbstractClassTransformerImpl {

	private static Log log = LogFactory.getLog( JavassistClassTransformer.class.getName() );

	public JavassistClassTransformer(ClassFilter classFilter, org.hibernate.bytecode.util.FieldFilter fieldFilter) {
		super( classFilter, fieldFilter );
	}

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
			log.error( "Unable to build enhancement metamodel for " + className );
			return classfileBuffer;
		}
		FieldTransformer transformer = getFieldTransformer( classfile );
		if ( transformer != null ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "Enhancing " + className );
			}
			DataOutputStream out = null;
			try {
				transformer.transform( classfile );
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
				out = new DataOutputStream( byteStream );
				classfile.write( out );
				return byteStream.toByteArray();
			}
			catch (Exception e) {
				log.error( "Unable to transform class", e );
				throw new HibernateException( "Unable to transform class: " + e.getMessage() );
			}
			finally {
				try {
					if ( out != null ) out.close();
				}
				catch (IOException e) {
					//swallow
				}
			}
		}
		return classfileBuffer;
	}

	protected FieldTransformer getFieldTransformer(final ClassFile classfile) {
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
				}
		);
	}

	private boolean alreadyInstrumented(ClassFile classfile) {
		String[] intfs = classfile.getInterfaces();
		for ( int i = 0; i < intfs.length; i++ ) {
			if ( FieldHandled.class.getName().equals( intfs[i] ) ) {
				return true;
			}
		}
		return false;
	}
}
