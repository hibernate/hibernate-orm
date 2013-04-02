package org.hibernate.ejb.criteria.jpaMapMode;

import org.hibernate.ejb.criteria.jpaMapMode.Multiplicity;
import org.hibernate.ejb.criteria.jpaMapMode.Property;
import org.hibernate.ejb.criteria.jpaMapMode.Relationship;
import org.hibernate.ejb.criteria.jpaMapMode.RelationshipColumnMapping;
import org.hibernate.ejb.criteria.jpaMapMode.DocumentInstance;
import org.hibernate.ejb.criteria.jpaMapMode.PropertyInstance;
import org.hibernate.ejb.criteria.jpaMapMode.RelationshipInstance;
import org.hibernate.HibernateException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.ejb.metamodel.MapMember;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.Setter;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * Tells hibernate how to get and set Hibernate properties in whatever class we use to hold documents.
 * We create an instance of this for each Document in the metamodel. It generates Hibernate
 * Getters and Setters for each property and relationship.
 */
@SuppressWarnings("rawtypes")
public class DocumentInstanceAccessor implements PropertyAccessor {
    private final PersistentDocumentClass persistentDocumentClass;

    public DocumentInstanceAccessor(PersistentDocumentClass persistentDocumentClass) {
        this.persistentDocumentClass = persistentDocumentClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Getter getGetter(Class theClass, String propertyName)
            throws PropertyNotFoundException {

        HibernateProperty hibernateProperty = (HibernateProperty) persistentDocumentClass.getProperty(propertyName);
        if (hibernateProperty instanceof HibernateRelationshipProperty) {
            HibernateRelationshipProperty hibernateRelationshipProperty = (HibernateRelationshipProperty) hibernateProperty;
            return new DocumentInstanceRelationshipGetter(persistentDocumentClass, hibernateRelationshipProperty.getRelationshipColumnMapping());
        }

        Property property = ((HibernateDocumentProperty) hibernateProperty).getPropertyColumnMapping().getProperty();

        return new DocumentInstancePropertyGetter(property);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Setter getSetter(Class theClass, String propertyName)
            throws PropertyNotFoundException {

        HibernateProperty hibernateProperty = (HibernateProperty) persistentDocumentClass.getProperty(propertyName);
        if (hibernateProperty instanceof HibernateRelationshipProperty) {
            HibernateRelationshipProperty hibernateRelationshipProperty = (HibernateRelationshipProperty) hibernateProperty;
            return new DocumentInstanceRelationshipSetter(persistentDocumentClass, hibernateRelationshipProperty.getRelationshipColumnMapping());
        }

        Property property = ((HibernateDocumentProperty) hibernateProperty).getPropertyColumnMapping().getProperty();

        return new DocumentInstancePropertySetter(property);
    }

    @SuppressWarnings("serial")
    public final class DocumentInstancePropertySetter implements Setter {
        private final Property property;

        DocumentInstancePropertySetter(Property property) {
            this.property = property;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Method getMethod() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMethodName() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void set(Object target, Object value,
                        SessionFactoryImplementor factory) throws HibernateException {

            PropertyInstance propertyInstance = new PropertyInstance(property,
                    value);
            ((DocumentInstance) target).setPropertyInstance(property,
                    propertyInstance);
        }

    }

    @SuppressWarnings("serial")
    public final class DocumentInstancePropertyGetter implements Getter {
        private final Property property;

        DocumentInstancePropertyGetter(Property property) {
            this.property = property;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Member getMember() {
            return new MapMember(property.getName(), property.getType().getJavaTypeForPropertyType());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Method getMethod() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMethodName() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object get(Object target) throws HibernateException {
            PropertyInstance propertyInstance = ((DocumentInstance) target)
                    .getPropertyInstance(property);
            if (propertyInstance != null) {
                return propertyInstance.getValue();
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getForInsert(Object target, Map mergeMap,
                                   SessionImplementor session) {
            return get(target);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class getReturnType() {
            return Object.class;
        }
    }

    @SuppressWarnings("serial")
    public static final class DocumentInstanceRelationshipSetter implements Setter {
        private final Relationship relationship;
        private final Relationship.Side side;

        DocumentInstanceRelationshipSetter(PersistentDocumentClass persistentDocumentClass, RelationshipColumnMapping relationshipColumnMapping) {
            this.relationship = relationshipColumnMapping.getRelationship();
            this.side = getSide(persistentDocumentClass, relationshipColumnMapping);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Method getMethod() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMethodName() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void set(Object target, Object value,
                        SessionFactoryImplementor factory) throws HibernateException {

            RelationshipInstance relationshipInstance = new RelationshipInstance(relationship,
                    value);
            ((DocumentInstance) target).setRelationshipInstance(relationship,
                    relationshipInstance);
        }

    }

    protected static Relationship.Side getSide(PersistentDocumentClass persistentDocumentClass, RelationshipColumnMapping relationshipColumnMapping) {
        final Relationship.Side side = relationshipColumnMapping.getSide();
        return persistentDocumentClass.getDocumentTableMapping() == relationshipColumnMapping.getDocumentTableMapping() ?
                side : side.oppositeSide();
    }

    /**
     * Getters for relationships. This will be much easier when relationships are treated like
     * any other properties.
     */
    @SuppressWarnings("serial")
    public static final class DocumentInstanceRelationshipGetter implements Getter {
        private final Relationship relationship;
        private final Relationship.Side side;

        DocumentInstanceRelationshipGetter(PersistentDocumentClass persistentDocumentClass, RelationshipColumnMapping relationshipColumnMapping) {
            this.relationship = relationshipColumnMapping.getRelationship();
            this.side = getSide(persistentDocumentClass, relationshipColumnMapping);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Member getMember() {
            return new MapMember(relationship.getRoleName(side), getType());
        }

        public Class<?> getType() {
            final Multiplicity multiplicity = relationship.getMultiplicity(side);
            if (multiplicity == Multiplicity.ONE_OR_MORE || multiplicity == Multiplicity.ZERO_OR_MORE) {
                return Set.class;
            }

            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Method getMethod() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMethodName() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object get(Object target) throws HibernateException {
            RelationshipInstance relationshipInstance = ((DocumentInstance) target)
                    .getRelationshipInstance(relationship);
            if (relationshipInstance != null) {
                return relationshipInstance.getValue();
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getForInsert(Object target, Map mergeMap,
                                   SessionImplementor session) {
            return get(target);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class getReturnType() {
            return Object.class;
        }
    }

}
