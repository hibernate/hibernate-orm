//$Id: InstrumentTask.java 10206 2006-08-03 19:59:42Z steve.ebersole@jboss.com $
package org.hibernate.tool.instrument.cglib;

import org.hibernate.bytecode.util.BasicClassFilter;
import org.hibernate.bytecode.util.ClassDescriptor;
import org.hibernate.bytecode.cglib.BytecodeProviderImpl;
import org.hibernate.bytecode.ClassTransformer;
import org.hibernate.tool.instrument.BasicInstrumentationTask;
import org.hibernate.repackage.cglib.asm.ClassReader;

import java.io.ByteArrayInputStream;

import org.hibernate.repackage.cglib.core.ClassNameReader;
import org.hibernate.repackage.cglib.transform.impl.InterceptFieldEnabled;

/**
 * An Ant task for instrumenting persistent classes in order to enable
 * field-level interception using CGLIB.
 * <p/>
 * In order to use this task, typically you would define a a taskdef
 * similiar to:<pre>
 * <taskdef name="instrument" classname="org.hibernate.tool.instrument.cglib.InstrumentTask">
 *     <classpath refid="lib.class.path"/>
 * </taskdef>
 * </pre>
 * where <tt>lib.class.path</tt> is an ANT path reference containing all the
 * required Hibernate and CGLIB libraries.
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
 * @author Gavin King
 * @author Steve Ebersole
 */
public class InstrumentTask extends BasicInstrumentationTask {

	private static final BasicClassFilter CLASS_FILTER = new BasicClassFilter();

	private final BytecodeProviderImpl provider = new BytecodeProviderImpl();


	protected ClassDescriptor getClassDescriptor(byte[] byecode) throws Exception {
		return new CustomClassDescriptor( byecode );
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
		private final byte[] bytecode;
		private final String name;
		private final boolean isInstrumented;

		public CustomClassDescriptor(byte[] bytecode) throws Exception {
			this.bytecode = bytecode;
			ClassReader reader = new ClassReader( new ByteArrayInputStream( bytecode ) );
			String[] names = ClassNameReader.getClassInfo( reader );
			this.name = names[0];
			boolean instrumented = false;
			for ( int i = 1; i < names.length; i++ ) {
				if ( InterceptFieldEnabled.class.getName().equals( names[i] ) ) {
					instrumented = true;
					break;
				}
			}
			this.isInstrumented = instrumented;
		}

		public String getName() {
			return name;
		}

		public boolean isInstrumented() {
			return isInstrumented;
		}

		public byte[] getBytes() {
			return bytecode;
		}
	}


}
