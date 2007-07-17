// $Id: VersionProperty.java 10119 2006-07-14 00:09:19Z steve.ebersole@jboss.com $
package org.hibernate.tuple;

import org.hibernate.engine.CascadeStyle;
import org.hibernate.engine.VersionValue;
import org.hibernate.type.Type;

/**
 * Represents a version property within the Hibernate runtime-metamodel.
 *
 * @author Steve Ebersole
 */
public class VersionProperty extends StandardProperty {

    private final VersionValue unsavedValue;

    /**
     * Constructs VersionProperty instances.
     *
     * @param name The name by which the property can be referenced within
     * its owner.
     * @param node The node name to use for XML-based representation of this
     * property.
     * @param type The Hibernate Type of this property.
     * @param lazy Should this property be handled lazily?
     * @param insertable Is this property an insertable value?
     * @param updateable Is this property an updateable value?
     * @param insertGenerated Is this property generated in the database on insert?
     * @param updateGenerated Is this property generated in the database on update?
     * @param nullable Is this property a nullable value?
     * @param checkable Is this property a checkable value?
     * @param versionable Is this property a versionable value?
     * @param cascadeStyle The cascade style for this property's value.
     * @param unsavedValue The value which, if found as the value of
     * this (i.e., the version) property, represents new (i.e., un-saved)
     * instances of the owning entity.
     */
    public VersionProperty(
            String name,
            String node,
            Type type,
            boolean lazy,
            boolean insertable,
            boolean updateable,
            boolean insertGenerated,
            boolean updateGenerated,
            boolean nullable,
            boolean checkable,
            boolean versionable,
            CascadeStyle cascadeStyle,
            VersionValue unsavedValue) {
        super( name, node, type, lazy, insertable, updateable, insertGenerated, updateGenerated, nullable, checkable, versionable, cascadeStyle, null );
        this.unsavedValue = unsavedValue;
    }

    public VersionValue getUnsavedValue() {
        return unsavedValue;
    }
}
