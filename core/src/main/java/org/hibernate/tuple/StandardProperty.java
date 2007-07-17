// $Id: StandardProperty.java 10119 2006-07-14 00:09:19Z steve.ebersole@jboss.com $
package org.hibernate.tuple;

import org.hibernate.engine.CascadeStyle;
import org.hibernate.type.Type;
import org.hibernate.FetchMode;

/**
 * Represents a basic property within the Hibernate runtime-metamodel.
 *
 * @author Steve Ebersole
 */
public class StandardProperty extends Property {

    private final boolean lazy;
    private final boolean insertable;
    private final boolean updateable;
	private final boolean insertGenerated;
	private final boolean updateGenerated;
    private final boolean nullable;
    private final boolean dirtyCheckable;
    private final boolean versionable;
    private final CascadeStyle cascadeStyle;
	private final FetchMode fetchMode;

    /**
     * Constructs StandardProperty instances.
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
     * @param fetchMode Any fetch mode defined for this property
     */
    public StandardProperty(
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
            FetchMode fetchMode) {
        super(name, node, type);
        this.lazy = lazy;
        this.insertable = insertable;
        this.updateable = updateable;
        this.insertGenerated = insertGenerated;
	    this.updateGenerated = updateGenerated;
        this.nullable = nullable;
        this.dirtyCheckable = checkable;
        this.versionable = versionable;
        this.cascadeStyle = cascadeStyle;
	    this.fetchMode = fetchMode;
    }

    public boolean isLazy() {
        return lazy;
    }

    public boolean isInsertable() {
        return insertable;
    }

    public boolean isUpdateable() {
        return updateable;
    }

	public boolean isInsertGenerated() {
		return insertGenerated;
	}

	public boolean isUpdateGenerated() {
		return updateGenerated;
	}

    public boolean isNullable() {
        return nullable;
    }

    public boolean isDirtyCheckable(boolean hasUninitializedProperties) {
        return isDirtyCheckable() && ( !hasUninitializedProperties || !isLazy() );
    }

    public boolean isDirtyCheckable() {
        return dirtyCheckable;
    }

    public boolean isVersionable() {
        return versionable;
    }

    public CascadeStyle getCascadeStyle() {
        return cascadeStyle;
    }

	public FetchMode getFetchMode() {
		return fetchMode;
	}
}
