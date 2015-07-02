/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.instrument;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.bytecode.buildtime.spi.ClassFilter;
import org.hibernate.bytecode.buildtime.spi.FieldFilter;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.cfg.Environment;

/**
 * Enhance the classes allowing them to implements InterceptFieldEnabled
 * This interface is then used by Hibernate for some optimizations.
 *
 * @author Emmanuel Bernard
 */
public class InterceptFieldClassFileTransformer implements javax.persistence.spi.ClassTransformer {
	private ClassTransformer classTransformer;

	public InterceptFieldClassFileTransformer(Collection<String> entities) {
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
					@Override
					public boolean shouldInstrumentField(String className, String fieldName) {
						return true;
					}
					@Override
					public boolean shouldTransformFieldAccess(
							String transformingClassName, String fieldOwnerClassName, String fieldName
					) {
						return true;
					}
				}
		);
	}
	@Override
	public byte[] transform(
			ClassLoader loader,
			String className,
			Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer ) throws IllegalClassFormatException {
		try {
			return classTransformer.transform( loader, className, classBeingRedefined,
					protectionDomain, classfileBuffer );
		}
		catch (Exception e) {
			throw new IllegalClassFormatException( e.getMessage() );
		}
	}
}
