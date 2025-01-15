/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.spi;

import org.hibernate.bytecode.spi.BytecodeProvider;

import jakarta.persistence.metamodel.Type;
import org.hibernate.Incubating;

/**
 * The context for performing an enhancement.  Enhancement can happen in any number of ways:<ul>
 * <li>Build time, via Ant</li>
 * <li>Build time, via Maven</li>
 * <li>Build time, via Gradle</li>
 * <li>Runtime, via agent</li>
 * <li>Runtime, via JPA constructs</li>
 * </ul>
 * <p>
 * This interface isolates the code that actually does the enhancement from the underlying context in which
 * the enhancement is being performed.
 *
 * @author Steve Ebersole
 */
public interface EnhancementContext {
	/**
	 * Obtain access to the ClassLoader that can be used to load Class references.  In JPA SPI terms, this
	 * should be a "temporary class loader" as defined by
	 * {@link jakarta.persistence.spi.PersistenceUnitInfo#getNewTempClassLoader()}
	 *
	 * @return The class loader that the enhancer can use.
	 */
	ClassLoader getLoadingClassLoader();

	/**
	 * Does the given class descriptor represent an entity class?
	 *
	 * @param classDescriptor The descriptor of the class to check.
	 *
	 * @return {@code true} if the class is an entity; {@code false} otherwise.
	 */
	boolean isEntityClass(UnloadedClass classDescriptor);

	/**
	 * Does the given class name represent an embeddable/component class?
	 *
	 * @param classDescriptor The descriptor of the class to check.
	 *
	 * @return {@code true} if the class is an embeddable/component; {@code false} otherwise.
	 */
	boolean isCompositeClass(UnloadedClass classDescriptor);

	/**
	 * Does the given class name represent a MappedSuperclass class?
	 *
	 * @param classDescriptor The descriptor of the class to check.
	 *
	 * @return {@code true} if the class is a mapped super class; {@code false} otherwise.
	 */
	boolean isMappedSuperclassClass(UnloadedClass classDescriptor);

	/**
	 * Should we manage association of bi-directional persistent attributes for this field?
	 *
	 * @param field The field to check.
	 *
	 * @return {@code true} indicates that the field is enhanced so that for bi-directional persistent fields
	 * 			the association is managed, i.e. the associations are automatically set; {@code false} indicates that
	 * 			the management is handled by the user.
	 */
	boolean doBiDirectionalAssociationManagement(UnloadedField field);

	/**
	 * Should we in-line dirty checking for persistent attributes for this class?
	 *
	 * @param classDescriptor The descriptor of the class to check.
	 *
	 * @return {@code true} indicates that dirty checking should be in-lined within the entity; {@code false}
	 *         indicates it should not.  In-lined is more easily serializable and probably more performant.
	 * @deprecated Will be removed without replacement. See HHH-15641
	 */
	@Deprecated(forRemoval = true)
	boolean doDirtyCheckingInline(UnloadedClass classDescriptor);

	/**
	 * Should we enhance field access to entities from this class?
	 *
	 * @param classDescriptor The descriptor of the class to check.
	 *
	 * @return {@code true} indicates that any direct access to fields of entities should be routed to the enhanced
	 *         getter / setter  method.
	 */
	boolean doExtendedEnhancement(UnloadedClass classDescriptor);

	/**
	 * Does the given class define any lazy loadable attributes?
	 *
	 * @param classDescriptor The class to check
	 *
	 * @return true/false
	 * @deprecated Will be removed without replacement. See HHH-15641
	 */
	@Deprecated(forRemoval = true)
	boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor);

	// todo : may be better to invert these 2 such that the context is asked for an ordered list of persistent fields for an entity/composite

	/**
	 * Does the field represent persistent state?  Persistent fields will be "enhanced".
	 * <p>
	 * may be better to perform basic checks in the caller (non-static, etc) and call out with just the
	 * Class name and field name...
	 *
	 * @param ctField The field reference.
	 *
	 * @return {@code true} if the field is ; {@code false} otherwise.
	 */
	boolean isPersistentField(UnloadedField ctField);

	/**
	 * For fields which are persistent (according to {@link #isPersistentField}), determine the corresponding ordering
	 * maintained within the Hibernate metamodel.
	 *
	 * @param persistentFields The persistent field references.
	 *
	 * @return The ordered references.
	 */
	UnloadedField[] order(UnloadedField[] persistentFields);

	/**
	 * Determine if a field is lazy loadable.
	 *
	 * @param field The field to check
	 *
	 * @return {@code true} if the field is lazy loadable; {@code false} otherwise.
	 */
	boolean isLazyLoadable(UnloadedField field);

	/**
	 * @param field the field to check
	 *
	 * @return {@code true} if the field is mapped
	 */
	boolean isMappedCollection(UnloadedField field);

	boolean isDiscoveredType(UnloadedClass classDescriptor);

	void registerDiscoveredType(UnloadedClass classDescriptor, Type.PersistenceType type);

	/**
	 * @return The expected behavior when encountering a class that cannot be enhanced,
	 * in particular when attribute names don't match field names.
	 * @see <a href="https://hibernate.atlassian.net/browse/HHH-16572">HHH-16572</a>
	 * @see <a href="https://hibernate.atlassian.net/browse/HHH-18833">HHH-18833</a>
	 */
	@Incubating
	default UnsupportedEnhancementStrategy getUnsupportedEnhancementStrategy() {
		return UnsupportedEnhancementStrategy.SKIP;
	}

	/**
	 * Allows to force the use of a specific instance of BytecodeProvider to perform the enhancement.
	 * @return When returning {code null} the default implementation will be used. Only return a different instance if
	 * you need to override the default implementation.
	 */
	@Incubating
	default BytecodeProvider getBytecodeProvider() {
		return null;
	}

}
