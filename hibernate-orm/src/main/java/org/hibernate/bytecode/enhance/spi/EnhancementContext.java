/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.spi;

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
	public boolean isEntityClass(UnloadedClass classDescriptor);

	/**
	 * Does the given class name represent an embeddable/component class?
	 *
	 * @param classDescriptor The descriptor of the class to check.
	 *
	 * @return {@code true} if the class is an embeddable/component; {@code false} otherwise.
	 */
	public boolean isCompositeClass(UnloadedClass classDescriptor);

	/**
	 * Does the given class name represent an MappedSuperclass class?
	 *
	 * @param classDescriptor The descriptor of the class to check.
	 *
	 * @return {@code true} if the class is an mapped super class; {@code false} otherwise.
	 */
	public boolean isMappedSuperclassClass(UnloadedClass classDescriptor);

	/**
	 * Should we manage association of bi-directional persistent attributes for this field?
	 *
	 * @param field The field to check.
	 *
	 * @return {@code true} indicates that the field is enhanced so that for bi-directional persistent fields
	 * 			the association is managed, i.e. the associations are automatically set; {@code false} indicates that
	 * 			the management is handled by the user.
	 */
	public boolean doBiDirectionalAssociationManagement(UnloadedField field);

	/**
	 * Should we in-line dirty checking for persistent attributes for this class?
	 *
	 * @param classDescriptor The descriptor of the class to check.
	 *
	 * @return {@code true} indicates that dirty checking should be in-lined within the entity; {@code false}
	 *         indicates it should not.  In-lined is more easily serializable and probably more performant.
	 */
	public boolean doDirtyCheckingInline(UnloadedClass classDescriptor);

	/**
	 * Should we enhance field access to entities from this class?
	 *
	 * @param classDescriptor The descriptor of the class to check.
	 *
	 * @return {@code true} indicates that any direct access to fields of entities should be routed to the enhanced
	 *         getter / setter  method.
	 */
	public boolean doExtendedEnhancement(UnloadedClass classDescriptor);

	/**
	 * Does the given class define any lazy loadable attributes?
	 *
	 * @param classDescriptor The class to check
	 *
	 * @return true/false
	 */
	public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor);

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
	public boolean isPersistentField(UnloadedField ctField);

	/**
	 * For fields which are persistent (according to {@link #isPersistentField}), determine the corresponding ordering
	 * maintained within the Hibernate metamodel.
	 *
	 * @param persistentFields The persistent field references.
	 *
	 * @return The ordered references.
	 */
	public UnloadedField[] order(UnloadedField[] persistentFields);

	/**
	 * Determine if a field is lazy loadable.
	 *
	 * @param field The field to check
	 *
	 * @return {@code true} if the field is lazy loadable; {@code false} otherwise.
	 */
	public boolean isLazyLoadable(UnloadedField field);

	/**
	 * @param field the field to check
	 *
	 * @return {@code true} if the field is mapped
	 */
	public boolean isMappedCollection(UnloadedField field);
}
