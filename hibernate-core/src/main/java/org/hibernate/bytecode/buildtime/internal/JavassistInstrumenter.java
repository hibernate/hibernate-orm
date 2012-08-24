/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.bytecode.buildtime.internal;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.bytecode.ClassFile;

import org.hibernate.bytecode.buildtime.spi.AbstractInstrumenter;
import org.hibernate.bytecode.buildtime.spi.BasicClassFilter;
import org.hibernate.bytecode.buildtime.spi.ClassDescriptor;
import org.hibernate.bytecode.buildtime.spi.Logger;
import org.hibernate.bytecode.internal.javassist.BytecodeProviderImpl;
import org.hibernate.bytecode.internal.javassist.FieldHandled;
import org.hibernate.bytecode.spi.ClassTransformer;

/**
 * Strategy for performing build-time instrumentation of persistent classes in order to enable
 * field-level interception using Javassist.
 *
 * @author Steve Ebersole
 * @author Muga Nishizawa
 * @author Dustin Schultz
 */
public class JavassistInstrumenter extends AbstractInstrumenter {

	private static final BasicClassFilter CLASS_FILTER = new BasicClassFilter();

	private final BytecodeProviderImpl provider = new BytecodeProviderImpl();

	public JavassistInstrumenter(Logger logger, Options options) {
		super( logger, options );
	}

	@Override
    protected ClassDescriptor getClassDescriptor(byte[] bytecode) throws IOException {
		return new CustomClassDescriptor( bytecode );
	}

	@Override
    protected ClassTransformer getClassTransformer(ClassDescriptor descriptor, Set classNames) {
		if ( descriptor.isInstrumented() ) {
			logger.debug( "class [" + descriptor.getName() + "] already instrumented" );
			return null;
		}
		else {
			return provider.getTransformer( CLASS_FILTER, new CustomFieldFilter( descriptor, classNames ) );
		}
	}
	
	@Override
	public void execute(Set<File> files) {
		ClassPool cp = ClassPool.getDefault();
		cp.insertClassPath(new ClassClassPath(this.getClass()));
		try {
			for (File file : files) {
				cp.makeClass(new FileInputStream(file));
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		super.execute(files);
	}

	private static class CustomClassDescriptor implements ClassDescriptor {
		private final byte[] bytes;
		private final ClassFile classFile;

		public CustomClassDescriptor(byte[] bytes) throws IOException {
			this.bytes = bytes;
			this.classFile = new ClassFile( new DataInputStream( new ByteArrayInputStream( bytes ) ) );
		}

		public String getName() {
			return classFile.getName();
		}

		public boolean isInstrumented() {
			String[] interfaceNames = classFile.getInterfaces();
			for ( String interfaceName : interfaceNames ) {
				if ( FieldHandled.class.getName().equals( interfaceName ) ) {
					return true;
				}
			}
			return false;
		}

		public byte[] getBytes() {
			return bytes;
		}
	}

}
