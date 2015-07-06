/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.enhance;

import java.util.Collection;

import javassist.CtClass;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;

/**
 * @author Steve Ebersole
 */
public class EnhancementContextImpl extends DefaultEnhancementContext {
	private final Collection<String> classNames;
	private final ClassLoader classLoader;

	public EnhancementContextImpl(Collection<String> classNames, ClassLoader classLoader) {
		this.classNames = classNames;
		this.classLoader = classLoader;
	}

	@Override
	public ClassLoader getLoadingClassLoader() {
		return classLoader;
	}

	@Override
	public boolean isEntityClass(CtClass classDescriptor) {
		return classNames.contains( classDescriptor.getName() )
				&& super.isEntityClass( classDescriptor );
	}

	@Override
	public boolean isCompositeClass(CtClass classDescriptor) {
		return classNames.contains( classDescriptor.getName() )
				&& super.isCompositeClass( classDescriptor );
	}
}
