package org.hibernate.envers.revisioninfo;

import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.tools.reflection.ReflectionTools;
import org.hibernate.property.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Returns modified entity types from a persisted revision info entity.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ModifiedEntityTypesReader {
    private final Getter modifiedEntityTypesGetter;

    public ModifiedEntityTypesReader(Class<?> revisionInfoClass, PropertyData modifiedEntityTypesData) {
        modifiedEntityTypesGetter = ReflectionTools.getGetter(revisionInfoClass, modifiedEntityTypesData);
    }

    @SuppressWarnings({"unchecked"})
    public Set<Class> getModifiedEntityTypes(Object revisionEntity) {
        // The default mechanism of tracking entity types that have been changed during each revision, stores
        // fully qualified Java class names.
        Set<String> modifiedEntityClassNames = (Set<String>) modifiedEntityTypesGetter.get(revisionEntity);
        if (modifiedEntityClassNames != null) {
            Set<Class> result = new HashSet<Class>(modifiedEntityClassNames.size());
            for (String entityClassName : modifiedEntityClassNames) {
                try {
                    result.add(Thread.currentThread().getContextClassLoader().loadClass(entityClassName));
                } catch (ClassNotFoundException e) {
                    // This shall never happen
                    throw new RuntimeException(e);
                }
            }
            return result;
        }
        return Collections.EMPTY_SET;
    }
}
