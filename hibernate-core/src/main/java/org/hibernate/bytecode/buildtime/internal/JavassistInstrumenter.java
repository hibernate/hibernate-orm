/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.buildtime.internal;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Set;

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
 */
public class JavassistInstrumenter extends AbstractInstrumenter {

	private static final BasicClassFilter CLASS_FILTER = new BasicClassFilter();

	private final BytecodeProviderImpl provider = new BytecodeProviderImpl();

	/**
	 * Constructs the Javassist-based instrumenter.
	 *
	 * @param logger Logger to use
	 * @param options Instrumentation options
	 */
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
			final String[] interfaceNames = classFile.getInterfaces();
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
