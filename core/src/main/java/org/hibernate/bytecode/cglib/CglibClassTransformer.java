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
package org.hibernate.bytecode.cglib;

import java.security.ProtectionDomain;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

import org.hibernate.repackage.cglib.transform.ClassTransformer;
import org.hibernate.repackage.cglib.transform.TransformingClassGenerator;
import org.hibernate.repackage.cglib.transform.ClassReaderGenerator;
import org.hibernate.repackage.cglib.transform.impl.InterceptFieldEnabled;
import org.hibernate.repackage.cglib.transform.impl.InterceptFieldFilter;
import org.hibernate.repackage.cglib.transform.impl.InterceptFieldTransformer;
import org.hibernate.repackage.cglib.core.ClassNameReader;
import org.hibernate.repackage.cglib.core.DebuggingClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.bytecode.AbstractClassTransformerImpl;
import org.hibernate.bytecode.util.FieldFilter;
import org.hibernate.bytecode.util.ClassFilter;
import org.hibernate.HibernateException;
import org.hibernate.repackage.cglib.asm.Attribute;
import org.hibernate.repackage.cglib.asm.Type;
import org.hibernate.repackage.cglib.asm.ClassReader;
import org.hibernate.repackage.cglib.asm.ClassWriter;
import org.hibernate.repackage.cglib.asm.attrs.Attributes;

/**
 * Enhance the classes allowing them to implements InterceptFieldEnabled
 * This interface is then used by Hibernate for some optimizations.
 *
 * @author Emmanuel Bernard
 */
public class CglibClassTransformer extends AbstractClassTransformerImpl {

	private static Logger log = LoggerFactory.getLogger( CglibClassTransformer.class.getName() );

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
