package org.hibernate.tool.instrument.javassist;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import javassist.bytecode.ClassFile;

import org.hibernate.tool.instrument.BasicInstrumentationTask;
import org.hibernate.bytecode.util.ClassDescriptor;
import org.hibernate.bytecode.util.BasicClassFilter;
import org.hibernate.bytecode.ClassTransformer;
import org.hibernate.bytecode.javassist.BytecodeProviderImpl;
import org.hibernate.bytecode.javassist.FieldHandled;

/**
 * An Ant task for instrumenting persistent classes in order to enable
 * field-level interception using Javassist.
 * <p/>
 * In order to use this task, typically you would define a a taskdef
 * similiar to:<pre>
 * <taskdef name="instrument" classname="org.hibernate.tool.instrument.javassist.InstrumentTask">
 *     <classpath refid="lib.class.path"/>
 * </taskdef>
 * </pre>
 * where <tt>lib.class.path</tt> is an ANT path reference containing all the
 * required Hibernate and Javassist libraries.
 * <p/>
 * And then use it like:<pre>
 * <instrument verbose="true">
 *     <fileset dir="${testclasses.dir}/org/hibernate/test">
 *         <include name="yadda/yadda/**"/>
 *         ...
 *     </fileset>
 * </instrument>
 * </pre>
 * where the nested ANT fileset includes the class you would like to have
 * instrumented.
 * <p/>
 * Optionally you can chose to enable "Extended Instrumentation" if desired
 * by specifying the extended attriubute on the task:<pre>
 * <instrument verbose="true" extended="true">
 *     ...
 * </instrument>
 * </pre>
 * See the Hibernate manual regarding this option.
 *
 * @author Muga Nishizawa
 * @author Steve Ebersole
 */
public class InstrumentTask extends BasicInstrumentationTask {

	private static final BasicClassFilter CLASS_FILTER = new BasicClassFilter();

	private final BytecodeProviderImpl provider = new BytecodeProviderImpl();

	protected ClassDescriptor getClassDescriptor(byte[] bytecode) throws IOException {
		return new CustomClassDescriptor( bytecode );
	}

	protected ClassTransformer getClassTransformer(ClassDescriptor descriptor) {
		if ( descriptor.isInstrumented() ) {
			logger.verbose( "class [" + descriptor.getName() + "] already instrumented" );
			return null;
		}
		else {
			return provider.getTransformer( CLASS_FILTER, new CustomFieldFilter( descriptor ) );
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
			String[] intfs = classFile.getInterfaces();
			for ( int i = 0; i < intfs.length; i++ ) {
				if ( FieldHandled.class.getName().equals( intfs[i] ) ) {
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
