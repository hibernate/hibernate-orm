/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.bytecode.bytebuddy;

import java.util.function.Consumer;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl.EnhancerClassFileLocator;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl.ModelProcessingContextImpl;

import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;

import net.bytebuddy.pool.TypePool;

/**
 * @author Steve Ebersole
 */
public class Helper {
	public static void withProcessingContext(Consumer<ModelProcessingContextImpl> action) {
		withProcessingContext( Helper.class.getClassLoader(), action );
	}

	public static void withProcessingContext(ClassLoader classLoader, Consumer<ModelProcessingContextImpl> action) {
		try (EnhancerClassFileLocator classFileLocator = new EnhancerClassFileLocator( classLoader )) {
			final EnhancerTestContext context = new EnhancerTestContext() {
				@Override
				public ClassLoader getLoadingClassLoader() {
					return classLoader;
				}
			};
			final ModelProcessingContextImpl processingContext = new ModelProcessingContextImpl(
					TypePool.Default.WithLazyResolution.of( classFileLocator ),
					context
			);

			action.accept( processingContext );
		}
	}
}
