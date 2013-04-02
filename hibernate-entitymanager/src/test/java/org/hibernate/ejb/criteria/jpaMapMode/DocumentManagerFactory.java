package org.hibernate.ejb.criteria.jpaMapMode;

import org.hibernate.cfg.Configuration;
import org.hibernate.ejb.EntityManagerFactoryImpl;
import org.hibernate.service.ServiceRegistry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.spi.PersistenceUnitTransactionType;

/**
 * Creates a JPA EntityManager for our DocumentManager. Manages mapping from Document metamodel
 * classes to JPA EntityType objects.
 */
public class DocumentManagerFactory extends EntityManagerFactoryImpl {
    private final Map<String, EntityType> propertyToEntityType;

    public DocumentManagerFactory(PersistenceUnitTransactionType transactionType, boolean discardOnClose, Class sessionInterceptorClass, Configuration cfg, ServiceRegistry serviceRegistry, String persistenceUnitName) {
        super(transactionType, discardOnClose, sessionInterceptorClass, cfg, serviceRegistry, persistenceUnitName);
        Metamodel metamodel = getMetamodel();
        Set<EntityType<?>> entities = metamodel.getEntities();
        Map<String, EntityType> propertyToEntityType = new HashMap<String, EntityType>(entities.size());
        for (EntityType entityType : entities) {
            propertyToEntityType.put(entityType.getName(), entityType);
        }
        this.propertyToEntityType = Collections.unmodifiableMap(propertyToEntityType);
    }

    public EntityType getEntityType(Document document) {
        return propertyToEntityType.get(document.getName());
    }

}
