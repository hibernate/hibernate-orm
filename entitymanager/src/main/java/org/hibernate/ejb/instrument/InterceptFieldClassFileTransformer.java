//$Id$
package org.hibernate.ejb.instrument;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.bytecode.util.ClassFilter;
import org.hibernate.bytecode.util.FieldFilter;
import org.hibernate.cfg.Environment;

/**
 * Enhance the classes allowing them to implements InterceptFieldEnabled
 * This interface is then used by Hibernate for some optimizations.
 *
 * @author Emmanuel Bernard
 */
public class InterceptFieldClassFileTransformer implements javax.persistence.spi.ClassTransformer {
	private org.hibernate.bytecode.ClassTransformer classTransformer;

	public InterceptFieldClassFileTransformer(List<String> entities) {
//		classTransformer = Environment.getBytecodeProvider().getEntityClassTransformer(
//				null, entities.toArray( new String[ entities.size() ] )
//		);
		final List<String> copyEntities = new ArrayList<String>( entities.size() );
		copyEntities.addAll( entities );
		classTransformer = Environment.getBytecodeProvider().getTransformer(
				//TODO change it to a static class to make it faster?
				new ClassFilter() {
					public boolean shouldInstrumentClass(String className) {
						return copyEntities.contains( className );
					}
				},
				//TODO change it to a static class to make it faster?
				new FieldFilter() {

					public boolean shouldInstrumentField(String className, String fieldName) {
						return true;
					}

					public boolean shouldTransformFieldAccess(
							String transformingClassName, String fieldOwnerClassName, String fieldName
					) {
						return true;
					}
				}
		);
	}

	public byte[]
			transform(
			ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer
	) throws IllegalClassFormatException {
		try {
			return classTransformer.transform( loader, className, classBeingRedefined,
					protectionDomain, classfileBuffer );
		}
		catch (Exception e) {
			throw new IllegalClassFormatException( e.getMessage() );
		}
	}
}
