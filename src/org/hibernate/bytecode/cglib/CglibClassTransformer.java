//$Id: $
package org.hibernate.bytecode.cglib;

import java.security.ProtectionDomain;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

import net.sf.cglib.transform.ClassTransformer;
import net.sf.cglib.transform.TransformingClassGenerator;
import net.sf.cglib.transform.ClassReaderGenerator;
import net.sf.cglib.transform.impl.InterceptFieldEnabled;
import net.sf.cglib.transform.impl.InterceptFieldFilter;
import net.sf.cglib.transform.impl.InterceptFieldTransformer;
import net.sf.cglib.core.ClassNameReader;
import net.sf.cglib.core.DebuggingClassWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.bytecode.AbstractClassTransformerImpl;
import org.hibernate.bytecode.util.FieldFilter;
import org.hibernate.bytecode.util.ClassFilter;
import org.hibernate.HibernateException;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Type;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.attrs.Attributes;

/**
 * Enhance the classes allowing them to implements InterceptFieldEnabled
 * This interface is then used by Hibernate for some optimizations.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class CglibClassTransformer extends AbstractClassTransformerImpl {

	private static Log log = LogFactory.getLog( CglibClassTransformer.class.getName() );

	public CglibClassTransformer(ClassFilter classFilter, FieldFilter fieldFilter) {
		super( classFilter, fieldFilter );
	}

	protected byte[] doTransform(
			ClassLoader loader,
			String className,
			Class classBeingRedefined,
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer) {
		ClassReader reader;
		try {
			reader = new ClassReader( new ByteArrayInputStream( classfileBuffer ) );
		}
		catch (IOException e) {
			log.error( "Unable to read class", e );
			throw new HibernateException( "Unable to read class: " + e.getMessage() );
		}

		String[] names = ClassNameReader.getClassInfo( reader );
		ClassWriter w = new DebuggingClassWriter( true );
		ClassTransformer t = getClassTransformer( names );
		if ( t != null ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "Enhancing " + className );
			}
			ByteArrayOutputStream out;
			byte[] result;
			try {
				reader = new ClassReader( new ByteArrayInputStream( classfileBuffer ) );
				new TransformingClassGenerator(
						new ClassReaderGenerator( reader, attributes(), skipDebug() ), t
				).generateClass( w );
				out = new ByteArrayOutputStream();
				out.write( w.toByteArray() );
				result = out.toByteArray();
				out.close();
			}
			catch (Exception e) {
				log.error( "Unable to transform class", e );
				throw new HibernateException( "Unable to transform class: " + e.getMessage() );
			}
			return result;
		}
		return classfileBuffer;
	}


	private Attribute[] attributes() {
		return Attributes.getDefaultAttributes();
	}

	private boolean skipDebug() {
		return false;
	}

	private ClassTransformer getClassTransformer(final String[] classInfo) {
		if ( isAlreadyInstrumented( classInfo ) ) {
			return null;
		}
		return new InterceptFieldTransformer(
				new InterceptFieldFilter() {
					public boolean acceptRead(Type owner, String name) {
						return fieldFilter.shouldTransformFieldAccess( classInfo[0], owner.getClassName(), name );
					}

					public boolean acceptWrite(Type owner, String name) {
						return fieldFilter.shouldTransformFieldAccess( classInfo[0], owner.getClassName(), name );
					}
				}
		);
	}

	private boolean isAlreadyInstrumented(String[] classInfo) {
		for ( int i = 1; i < classInfo.length; i++ ) {
			if ( InterceptFieldEnabled.class.getName().equals( classInfo[i] ) ) {
				return true;
			}
		}
		return false;
	}
}
