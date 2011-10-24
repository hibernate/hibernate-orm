package org.hibernate.envers.revisioninfo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.tools.reflection.ReflectionTools;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;

/**
 * Automatically adds entity names, that have been changed during current revision, to revision entity.
 * @see ModifiedEntityNames
 * @see DefaultTrackingModifiedEntitiesRevisionEntity
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class DefaultTrackingModifiedEntitiesRevisionInfoGenerator extends DefaultRevisionInfoGenerator {
    private final Setter modifiedEntityNamesSetter;
    private final Getter modifiedEntityNamesGetter;

    public DefaultTrackingModifiedEntitiesRevisionInfoGenerator(String revisionInfoEntityName, Class<?> revisionInfoClass,
                                                                Class<? extends RevisionListener> listenerClass,
                                                                PropertyData revisionInfoTimestampData, boolean timestampAsDate,
                                                                PropertyData modifiedEntityNamesData) {
        super(revisionInfoEntityName, revisionInfoClass, listenerClass, revisionInfoTimestampData, timestampAsDate);
        modifiedEntityNamesSetter = ReflectionTools.getSetter(revisionInfoClass, modifiedEntityNamesData);
        modifiedEntityNamesGetter = ReflectionTools.getGetter(revisionInfoClass, modifiedEntityNamesData);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void entityChanged(Class entityClass, String entityName, Serializable entityId, RevisionType revisionType,
                              Object revisionEntity) {
        super.entityChanged(entityClass, entityName, entityId, revisionType, revisionEntity);
        Set<String> modifiedEntityNames = (Set<String>) modifiedEntityNamesGetter.get(revisionEntity);
        if (modifiedEntityNames == null) {
            modifiedEntityNames = new HashSet<String>();
            modifiedEntityNamesSetter.set(revisionEntity, modifiedEntityNames, null);
        }
        modifiedEntityNames.add(entityName);
    }
}
