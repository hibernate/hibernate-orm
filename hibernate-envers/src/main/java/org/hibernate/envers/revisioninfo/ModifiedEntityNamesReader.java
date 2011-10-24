package org.hibernate.envers.revisioninfo;

import java.util.Set;

import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.tools.reflection.ReflectionTools;
import org.hibernate.property.Getter;

/**
 * Returns modified entity names from a persisted revision info entity.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ModifiedEntityNamesReader {
    private final Getter modifiedEntityNamesGetter;

    public ModifiedEntityNamesReader(Class<?> revisionInfoClass, PropertyData modifiedEntityNamesData) {
        modifiedEntityNamesGetter = ReflectionTools.getGetter(revisionInfoClass, modifiedEntityNamesData);
    }

    @SuppressWarnings({"unchecked"})
    public Set<String> getModifiedEntityNames(Object revisionEntity) {
        return (Set<String>) modifiedEntityNamesGetter.get(revisionEntity);
    }
}
