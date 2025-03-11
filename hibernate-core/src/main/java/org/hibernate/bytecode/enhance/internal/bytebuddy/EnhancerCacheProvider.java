/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;

/**
 * A simple cache provider that allows overriding the resolution for the class that is currently being enhanced.
 */
final class EnhancerCacheProvider extends TypePool.CacheProvider.Simple {

	private final ThreadLocal<EnhancementState> enhancementState = new ThreadLocal<>();

	@Override
	public TypePool.Resolution find(final String name) {
		final EnhancementState enhancementState = getEnhancementState();
		if ( enhancementState != null && enhancementState.getClassName().equals( name ) ) {
			return enhancementState.getTypePoolResolution();
		}
		return super.find( name );
	}

	EnhancementState getEnhancementState() {
		return enhancementState.get();
	}

	void setEnhancementState(EnhancementState state) {
		enhancementState.set( state );
	}

	void removeEnhancementState() {
		enhancementState.remove();
	}

	static final class EnhancementState {
		private final String className;
		private final ClassFileLocator.Resolution classFileResolution;
		private TypePool.Resolution typePoolResolution;

		public EnhancementState(String className, ClassFileLocator.Resolution classFileResolution) {
			this.className = className;
			this.classFileResolution = classFileResolution;
		}

		public String getClassName() {
			return className;
		}

		public ClassFileLocator.Resolution getClassFileResolution() {
			return classFileResolution;
		}

		public TypePool.Resolution getTypePoolResolution() {
			return typePoolResolution;
		}

		public void setTypePoolResolution(TypePool.Resolution typePoolResolution) {
			this.typePoolResolution = typePoolResolution;
		}
	}
}
