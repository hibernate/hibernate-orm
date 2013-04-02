package org.hibernate.ejb.criteria.jpaMapMode;

import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.property.BasicPropertyAccessor;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.Setter;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.map.MapProxyFactory;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.entity.AbstractEntityTuplizer;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.jboss.logging.Logger;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Manages all the structures needed for Hibernate to convert from a row in a table to a DocumentInstance.
 * <p/>
 * Mostly pilfered from DynamicMapEntityTuplizer, which doesn't expose its
 * constructors. I would have subclassed it but that wasn't an option.
 *
 * @author Brad Koehn
 */
public class DocumentInstanceTuplizer extends AbstractEntityTuplizer {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(
            CoreMessageLogger.class, DocumentInstanceTuplizer.class.getName());

    private DocumentTableMapping documentTableMapping;
    private DocumentInstanceAccessor documentInstanceAccessor;

    DocumentInstanceTuplizer(EntityMetamodel entityMetamodel,
                             PersistentClass mappedEntity) {
        super(entityMetamodel, mappedEntity);
        PersistentDocumentClass persistentDocumentClass = (PersistentDocumentClass) mappedEntity;
        this.documentTableMapping = (persistentDocumentClass)
                .getDocumentTableMapping();
        this.documentInstanceAccessor = new DocumentInstanceAccessor(
                (PersistentDocumentClass) mappedEntity);
    }

    DocumentInstanceTuplizer(EntityMetamodel entityMetamodel,
                             EntityBinding mappedEntity) {
        super(entityMetamodel, mappedEntity);
        this.documentTableMapping = null;
        throw new IllegalStateException(
                "I really don't know how to handle this yet... set a breakpoint and debug.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityMode getEntityMode() {
        return EntityMode.MAP;
    }

    private PropertyAccessor buildPropertyAccessor(Property mappedProperty,
                                                   PersistentClass mappedEntity) {
        if (mappedProperty.isBackRef()) {
            return mappedProperty.getPropertyAccessor(null);
        } else {
            if (documentTableMapping == null) {
                this.documentTableMapping = ((PersistentDocumentClass) mappedEntity)
                        .getDocumentTableMapping();
            }
            if (documentInstanceAccessor == null) {
                documentInstanceAccessor = new DocumentInstanceAccessor(
                        (PersistentDocumentClass) mappedEntity);
            }
            return documentInstanceAccessor;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Getter buildPropertyGetter(Property mappedProperty,
                                         PersistentClass mappedEntity) {
        if (mappedProperty instanceof HibernateIdProperty) {
            return BasicPropertyAccessor.createGetter(
                    DocumentInstance.class, "id");
        }
        return buildPropertyAccessor(mappedProperty, mappedEntity).getGetter(
                null, mappedProperty.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Setter buildPropertySetter(Property mappedProperty,
                                         PersistentClass mappedEntity) {
        if (mappedProperty instanceof HibernateIdProperty) {
            return new BasicPropertyAccessor().getSetter(
                    DocumentInstance.class, "id");
        }
        return buildPropertyAccessor(mappedProperty, mappedEntity).getSetter(
                null, mappedProperty.getName());
    }

    static class UUIDSetter implements Setter {
        public static final Setter instance = new UUIDSetter();

        @Override
        public void set(Object target, Object value, SessionFactoryImplementor factory) throws HibernateException {
            String string = (String) value;
            UUID uuid = UUID.fromString(string);
            ((DocumentInstance) target).setId(uuid);
        }

        @Override
        public String getMethodName() {
            return "setId";  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Method getMethod() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Instantiator buildInstantiator(PersistentClass mappingInfo) {
        return new DocumentInstanceInstantiator(
                ((PersistentDocumentClass) mappingInfo)
                        .getDocumentTableMapping());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ProxyFactory buildProxyFactory(PersistentClass mappingInfo,
                                             Getter idGetter, Setter idSetter) {

        ProxyFactory pf = new MapProxyFactory();
        try {
            // TODO: design new lifecycle for ProxyFactory
            pf.postInstantiate(getEntityName(), null, null, null, null, null);
        } catch (HibernateException he) {
            LOG.unableToCreateProxyFactory(getEntityName(), he);
            pf = null;
        }
        return pf;
    }

    protected PropertyAccessor buildPropertyAccessor(
            AttributeBinding mappedProperty) {
        // TODO: fix when backrefs are working in new (Hibernate) metamodel
        // if ( mappedProperty.isBackRef() ) {
        // return mappedProperty.getPropertyAccessor( null );
        // }
        // else {

        return documentInstanceAccessor;
        // }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Getter buildPropertyGetter(AttributeBinding mappedProperty) {
        if (mappedProperty.getAttribute().getName().equals("id")) {
            return BasicPropertyAccessor.createGetter(
                    DocumentInstance.class, "id");
        }

        return buildPropertyAccessor(mappedProperty).getGetter(null,
                mappedProperty.getAttribute().getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Setter buildPropertySetter(AttributeBinding mappedProperty) {
        if (mappedProperty.getAttribute().getName().equals("id")) {
            return new BasicPropertyAccessor().getSetter(
                    DocumentInstance.class, "id");
        }
        return buildPropertyAccessor(mappedProperty).getSetter(null,
                mappedProperty.getAttribute().getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Instantiator buildInstantiator(EntityBinding mappingInfo) {
        mappingInfo.getEntity().getName();
        DocumentTableMapping documentTableMapping = null;
        return new DocumentInstanceInstantiator(documentTableMapping,
                mappingInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ProxyFactory buildProxyFactory(EntityBinding mappingInfo,
                                             Getter idGetter, Setter idSetter) {

        ProxyFactory pf = new MapProxyFactory();
        try {
            // TODO: design new lifecycle for ProxyFactory
            pf.postInstantiate(getEntityName(), null, null, null, null, null);
        } catch (HibernateException he) {
            LOG.unableToCreateProxyFactory(getEntityName(), he);
            pf = null;
        }
        return pf;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getMappedClass() {
        return DocumentInstance.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getConcreteProxyClass() {
        return DocumentInstance.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInstrumented() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityNameResolver[] getEntityNameResolvers() {
        return new EntityNameResolver[]{BasicEntityNameResolver.INSTANCE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String determineConcreteSubclassEntityName(Object entityInstance,
                                                      SessionFactoryImplementor factory) {
        return extractEmbeddedEntityName((DocumentInstance) entityInstance);
    }

    public static String extractEmbeddedEntityName(DocumentInstance entity) {
        return entity.getDocument().getName();
    }

    public static class BasicEntityNameResolver implements EntityNameResolver {
        public static final BasicEntityNameResolver INSTANCE = new BasicEntityNameResolver();

        /**
         * {@inheritDoc}
         */
        @Override
        public String resolveEntityName(Object entity) {
            if (!DocumentInstance.class.isInstance(entity)) {
                return null;
            }
            final String entityName = extractEmbeddedEntityName((DocumentInstance) entity);
            if (entityName == null) {
                throw new HibernateException(
                        "Could not determine type of dynamic map entity");
            }
            return entityName;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            return getClass().equals(obj.getClass());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }
}
