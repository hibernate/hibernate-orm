/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;
import org.hibernate.bytecode.enhance.internal.tracker.CompositeOwnerTracker;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.CompositeOwner;
import org.hibernate.engine.spi.CompositeTracker;

/**
 * enhancer for composite (embeddable) entities
 *
 * @author <a href="mailto:lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class CompositeEnhancer extends Enhancer {

	public CompositeEnhancer(EnhancementContext context) {
		super( context );
	}

	public void enhance(CtClass managedCtClass) {
		addInterceptorHandling( managedCtClass );

		if ( enhancementContext.doDirtyCheckingInline( managedCtClass ) ) {
			addInLineDirtyHandling( managedCtClass );
		}

		new PersistentAttributesEnhancer( enhancementContext ).enhance( managedCtClass );
	}

	/* --- */

	private void addInLineDirtyHandling(CtClass managedCtClass) {
		try {
			managedCtClass.addInterface( classPool.get( CompositeTracker.class.getName() ) );

			final CtClass compositeCtType = classPool.get( CompositeOwnerTracker.class.getName() );
			FieldWriter.addField( managedCtClass, compositeCtType, EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME );

			createCompositeTrackerMethod( managedCtClass );
		}
		catch (NotFoundException nfe) {
			nfe.printStackTrace();
		}
	}

	private void createCompositeTrackerMethod(CtClass managedCtClass) {
		try {
			MethodWriter.write( managedCtClass, "" +
							"public void %1$s(String name, %3$s tracker) {%n" +
							"  if (%2$s == null) { %2$s = new %4$s(); }%n" +
							"  %2$s.add(name, tracker);%n" +
							"}",
					EnhancerConstants.TRACKER_COMPOSITE_SET_OWNER,
					EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME,
					CompositeOwner.class.getName(),
					CompositeOwnerTracker.class.getName() );

			MethodWriter.write( managedCtClass, "" +
							"public void %1$s(String name) {%n" +
							"  if (%2$s != null) { %2$s.removeOwner(name); }%n" +
							"}",
					EnhancerConstants.TRACKER_COMPOSITE_CLEAR_OWNER,
					EnhancerConstants.TRACKER_COMPOSITE_FIELD_NAME );
		}
		catch (CannotCompileException cce) {
			cce.printStackTrace();
		}
	}

}
