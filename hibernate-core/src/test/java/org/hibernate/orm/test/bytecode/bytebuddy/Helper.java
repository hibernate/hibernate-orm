/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.bytecode.bytebuddy;

import java.util.function.Consumer;

import org.hibernate.bytecode.enhance.internal.bytebuddy.ClassFileLocatorImpl;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ManagedTypeModelContext;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl.ManagedTypeModelContextImpl;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl.ModelProcessingContextImpl;

import net.bytebuddy.pool.TypePool;

/**
 * @author Steve Ebersole
 */
public class Helper {
	public static void withProcessingContext(Consumer<ModelProcessingContextImpl> action) {
		withProcessingContext( Helper.class.getClassLoader(), action );
	}

	public static void withProcessingContext(ClassLoader classLoader, Consumer<ModelProcessingContextImpl> action) {
		try (ClassFileLocatorImpl classFileLocator = new ClassFileLocatorImpl( classLoader )) {
			final ModelProcessingContextImpl processingContext = new ModelProcessingContextImpl(
					classFileLocator,
					TypePool.Default.WithLazyResolution.of( classFileLocator )
			);

			action.accept( processingContext );
		}
	}

	public static void withManagedTypeModelContext(Consumer<ManagedTypeModelContext> action) {
		withManagedTypeModelContext( Helper.class.getClassLoader(), action );
	}

	public static void withManagedTypeModelContext(ClassLoader classLoader, Consumer<ManagedTypeModelContext> action) {
		try (ClassFileLocatorImpl classFileLocator = new ClassFileLocatorImpl( classLoader )) {
			final ModelProcessingContextImpl processingContext = new ModelProcessingContextImpl(
					classFileLocator,
					TypePool.Default.WithLazyResolution.of( classFileLocator )
			);
			action.accept( new ManagedTypeModelContextImpl( processingContext ) );
		}
	}
}
