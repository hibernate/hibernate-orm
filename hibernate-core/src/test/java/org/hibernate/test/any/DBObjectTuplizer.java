package org.hibernate.test.any;

import org.hibernate.EntityNameResolver;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.PojoEntityTuplizer;

/**
 * Tuplizer for {@link DBObject}
 * 
 * @author Nikolay Shestakov
 */
public class DBObjectTuplizer extends PojoEntityTuplizer {

    private static class DBObjectEntityNameResolver implements EntityNameResolver {
        @Override
        public String resolveEntityName(Object entity) {
            if (entity instanceof DBObject) {
                return ((DBObject) entity).getEntityName();
            }
            return null;
        }
    }

    public DBObjectTuplizer(EntityMetamodel entityMetamodel, EntityBinding mappedEntity) {
        super(entityMetamodel, mappedEntity);
    }

    public DBObjectTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
        super(entityMetamodel, mappedEntity);
    }

    @Override
    public EntityNameResolver[] getEntityNameResolvers() {
        return new EntityNameResolver[] { new DBObjectEntityNameResolver() };
    }
}
