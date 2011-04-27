package org.hibernate.envers.revisioninfo;

import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.tools.reflection.ReflectionTools;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Automatically adds and removes entity names changed during current revision.
 * @see org.hibernate.envers.ModifiedEntityNames
 * @see org.hibernate.envers.DefaultTrackingModifiedTypesRevisionEntity
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class DefaultTrackingModifiedTypesRevisionInfoGenerator extends DefaultRevisionInfoGenerator {
    private final Setter modifiedEntityNamesSetter;
    private final Getter modifiedEntityNamesGetter;

    public DefaultTrackingModifiedTypesRevisionInfoGenerator(String revisionInfoEntityName,
                                                             Class<?> revisionInfoClass,
                                                             Class<? extends RevisionListener> listenerClass,
                                                             PropertyData revisionInfoTimestampData, boolean timestampAsDate,
                                                             PropertyData modifiedEntityNamesData) {
        super(revisionInfoEntityName, revisionInfoClass, listenerClass, revisionInfoTimestampData, timestampAsDate);
        modifiedEntityNamesSetter = ReflectionTools.getSetter(revisionInfoClass, modifiedEntityNamesData);
        modifiedEntityNamesGetter = ReflectionTools.getGetter(revisionInfoClass, modifiedEntityNamesData);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void addEntityToRevision(String entityName, Object revisionInfo) {
        super.addEntityToRevision(entityName, revisionInfo);
        Set<String> modifiedEntityNames = (Set<String>) modifiedEntityNamesGetter.get(revisionInfo);
        if (modifiedEntityNames == null) {
            modifiedEntityNames = new HashSet<String>();
        }
        modifiedEntityNames.add(entityName);
        modifiedEntityNamesSetter.set(revisionInfo, modifiedEntityNames, null);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void removeEntityFromRevision(String entityName, Object revisionInfo) {
        super.removeEntityFromRevision(entityName, revisionInfo);
        Set<String> modifiedEntityNames = (Set<String>) modifiedEntityNamesGetter.get(revisionInfo);
        if (modifiedEntityNames == null) {
            return;
        }
        modifiedEntityNames.remove(entityName);
        modifiedEntityNamesSetter.set(revisionInfo, modifiedEntityNames, null);
    }
}
