/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
 */
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
