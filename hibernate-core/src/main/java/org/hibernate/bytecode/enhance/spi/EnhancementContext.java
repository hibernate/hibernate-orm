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
package org.hibernate.bytecode.enhance.spi;

import javassist.CtClass;
import javassist.CtField;

/**
 * The context for performing an enhancement.  Enhancement can happen in any number of ways:<ul>
 * <li>Build time, via Ant</li>
 * <li>Build time, via Maven</li>
 * <li>Build time, via Gradle</li>
 * <li>Runtime, via agent</li>
 * <li>Runtime, via JPA constructs</li>
 * </ul>
 * <p/>
 * This interface isolates the code that actually does the enhancement from the underlying context in which
 * the enhancement is being performed.
 *
 * @author Steve Ebersole
 * @todo Not sure its a great idea to expose Javassist classes this way.  maybe wrap them in our own contracts?
 */
public interface EnhancementContext {
	/**
	 * Obtain access to the ClassLoader that can be used to load Class references.  In JPA SPI terms, this
	 * should be a "temporary class loader" as defined by
	 * {@link javax.persistence.spi.PersistenceUnitInfo#getNewTempClassLoader()}
	 *
	 * @return The class loader that the enhancer can use.
	 */
	public ClassLoader getLoadingClassLoader();

	/**
	 * Does the given class descriptor represent a entity class?
	 *
	 * @param classDescriptor The descriptor of the class to check.
	 *
	 * @return {@code true} if the class is an entity; {@code false} otherwise.
	 */
	public boolean isEntityClass(CtClass classDescriptor);

	/**
	 * Does the given class name represent an embeddable/component class?
	 *
	 * @param classDescriptor The descriptor of the class to check.
	 *
	 * @return {@code true} if the class is an embeddable/component; {@code false} otherwise.
	 */
	public boolean isCompositeClass(CtClass classDescriptor);

	/**
	 * Should we in-line dirty checking for persistent attributes for this class?
	 *
	 * @param classDescriptor The descriptor of the class to check.
	 *
	 * @return {@code true} indicates that dirty checking should be in-lined within the entity; {@code false}
	 *         indicates it should not.  In-lined is more easily serializable and probably more performant.
	 */
	public boolean doDirtyCheckingInline(CtClass classDescriptor);

	/**
	 * Does the given class define any lazy loadable attributes?
	 *
	 * @param classDescriptor The class to check
	 *
	 * @return true/false
	 */
	public boolean hasLazyLoadableAttributes(CtClass classDescriptor);

	// todo : may be better to invert these 2 such that the context is asked for an ordered list of persistent fields for an entity/composite

	/**
	 * Does the field represent persistent state?  Persistent fields will be "enhanced".
	 * <p/>
	 * may be better to perform basic checks in the caller (non-static, etc) and call out with just the
	 * Class name and field name...
	 *
	 * @param ctField The field reference.
	 *
	 * @return {@code true} if the field is ; {@code false} otherwise.
	 */
	public boolean isPersistentField(CtField ctField);

	/**
	 * For fields which are persistent (according to {@link #isPersistentField}), determine the corresponding ordering
	 * maintained within the Hibernate metamodel.
	 *
	 * @param persistentFields The persistent field references.
	 *
	 * @return The ordered references.
	 */
	public CtField[] order(CtField[] persistentFields);

	/**
	 * Determine if a field is lazy loadable.
	 *
	 * @param field The field to check
	 *
	 * @return {@code true} if the field is lazy loadable; {@code false} otherwise.
	 */
	public boolean isLazyLoadable(CtField field);

	/**
	 * @param field the field to check
	 *
	 * @return {@code true} if the field is mapped
	 */
	public boolean isMappedCollection(CtField field);
}
