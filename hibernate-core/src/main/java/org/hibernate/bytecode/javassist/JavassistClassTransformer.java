/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.bytecode.javassist;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.ProtectionDomain;

import javassist.bytecode.ClassFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static Logger log = LoggerFactory.getLogger( JavassistClassTransformer.class.getName() );

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
