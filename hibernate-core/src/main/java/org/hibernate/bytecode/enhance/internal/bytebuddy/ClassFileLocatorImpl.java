/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import net.bytebuddy.dynamic.ClassFileLocator;

import static org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelSourceLogging.MODEL_SOURCE_LOGGER;
import static org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelSourceLogging.MODEL_SOURCE_TRACE_ENABLED;

/**
 * @author Steve Ebersole
 */
public class ClassFileLocatorImpl extends ClassFileLocator.ForClassLoader {
	// The name of the class to (possibly be) transformed.
	private String className;
	// The explicitly resolved Resolution for the class to (possibly be) transformed.
	private Resolution resolution;

	/**
	 * Creates a new class file locator for the given class loader.
	 *
	 * @param classLoader The class loader to query which must not be the bootstrap class loader, i.e. {@code null}.
	 */
	public ClassFileLocatorImpl(ClassLoader classLoader) {
		super( classLoader );
	}

	@Override
	public Resolution locate(String className) throws IOException {
		if ( MODEL_SOURCE_TRACE_ENABLED ) {
			MODEL_SOURCE_LOGGER.tracef( "ClassFileLocatorImpl#locate%s)", className );
		}
		if ( className.equals( this.className ) ) {
			return resolution;
		}
		else {
			return super.locate( className );
		}
	}

	void setClassNameAndBytes(String className, byte[] bytes) {
		if ( MODEL_SOURCE_TRACE_ENABLED ) {
			MODEL_SOURCE_LOGGER.tracef( "ClassFileLocatorImpl#setClassNameAndBytes%s)", className );
		}
		assert className != null;
		assert bytes != null;
		this.className = className;
		this.resolution = new Resolution.Explicit( bytes );
	}

	void setClassNameAndBytes(String className, Resolution resolution) {
		if ( MODEL_SOURCE_TRACE_ENABLED ) {
			MODEL_SOURCE_LOGGER.tracef( "ClassFileLocatorImpl#setClassNameAndBytes%s)", className );
		}
		assert className != null;
		assert resolution != null;
		this.className = className;
		this.resolution = resolution;
	}
}
