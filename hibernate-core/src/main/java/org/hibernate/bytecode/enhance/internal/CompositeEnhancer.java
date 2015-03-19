/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
