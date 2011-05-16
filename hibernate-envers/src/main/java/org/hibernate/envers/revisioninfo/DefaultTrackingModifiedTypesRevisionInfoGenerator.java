package org.hibernate.envers.revisioninfo;

import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.tools.reflection.ReflectionTools;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Automatically adds entity names changed during current revision.
 * @see org.hibernate.envers.ModifiedEntityTypes
 * @see org.hibernate.envers.DefaultTrackingModifiedTypesRevisionEntity
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class DefaultTrackingModifiedTypesRevisionInfoGenerator extends DefaultRevisionInfoGenerator {
    private final Setter modifiedEntityTypesSetter;
    private final Getter modifiedEntityTypesGetter;

    public DefaultTrackingModifiedTypesRevisionInfoGenerator(String revisionInfoEntityName,
                                                             Class<?> revisionInfoClass,
                                                             Class<? extends RevisionListener> listenerClass,
                                                             PropertyData revisionInfoTimestampData, boolean timestampAsDate,
                                                             PropertyData modifiedEntityTypesData) {
        super(revisionInfoEntityName, revisionInfoClass, listenerClass, revisionInfoTimestampData, timestampAsDate);
        modifiedEntityTypesSetter = ReflectionTools.getSetter(revisionInfoClass, modifiedEntityTypesData);
        modifiedEntityTypesGetter = ReflectionTools.getGetter(revisionInfoClass, modifiedEntityTypesData);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void entityChanged(Class entityClass, String entityName, Serializable entityId, RevisionType revisionType,
                              Object revisionEntity) {
        super.entityChanged(entityClass, entityName, entityId, revisionType, revisionEntity);
        Set<String> modifiedEntityTypes = (Set<String>) modifiedEntityTypesGetter.get(revisionEntity);
        if (modifiedEntityTypes == null) {
            modifiedEntityTypes = new HashSet<String>();
            modifiedEntityTypesSetter.set(revisionEntity, modifiedEntityTypes, null);
        }
        modifiedEntityTypes.add(entityClass.getName());
    }
}
