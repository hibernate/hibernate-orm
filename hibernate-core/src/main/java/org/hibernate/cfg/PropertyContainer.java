/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$

package org.hibernate.cfg;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.Target;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.cfg.annotations.HCANNHelper;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.jboss.logging.Logger;

import javax.persistence.*;
import java.util.*;

/**
 * A helper class to keep the {@code XProperty}s of a class ordered by access type.
 *
 * @author Hardy Ferentschik
 * modified by Nguyen Tho Tuan (@ngthotuan)
 * Will auto generate column order in database following the order of the properties
 * Ref: 
 */
class PropertyContainer {
//
//    static {
//        System.setProperty("jboss.i18n.generate-proxies", "true");
//    }

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, PropertyContainer.class.getName());

    /**
     * The class for which this container is created.
     */
    private final XClass xClass;
    private final XClass entityAtStake;

    /**
     * Holds the AccessType indicated for use at the class/container-level for cases where persistent attribute
     * did not specify.
     */
    private final AccessType classLevelAccessType;

    private final List<XProperty> persistentAttributes;

    PropertyContainer(XClass clazz, XClass entityAtStake, AccessType defaultClassLevelAccessType) {
        this.xClass = clazz;
        this.entityAtStake = entityAtStake;

        if (defaultClassLevelAccessType == AccessType.DEFAULT) {
            // this is effectively what the old code did when AccessType.DEFAULT was passed in
            // to getProperties(AccessType) from AnnotationBinder and InheritanceState
            defaultClassLevelAccessType = AccessType.PROPERTY;
        }

        AccessType localClassLevelAccessType = determineLocalClassDefinedAccessStrategy();
        assert localClassLevelAccessType != null;

        this.classLevelAccessType = localClassLevelAccessType != AccessType.DEFAULT
                ? localClassLevelAccessType
                : defaultClassLevelAccessType;
        assert classLevelAccessType == AccessType.FIELD || classLevelAccessType == AccessType.PROPERTY;


        final List<XProperty> fields = xClass.getDeclaredProperties(AccessType.FIELD.getType());
        final List<XProperty> getters = xClass.getDeclaredProperties(AccessType.PROPERTY.getType());

        preFilter(fields, getters);

        final Map<String, XProperty> persistentAttributesFromGetters = new HashMap<String, XProperty>();

        final LinkedHashMap<String, XProperty> localAttributeMap = new LinkedHashMap<>();
        collectPersistentAttributesUsingLocalAccessType(
                xClass,
                localAttributeMap,
                persistentAttributesFromGetters,
                fields,
                getters
        );
        collectPersistentAttributesUsingClassLevelAccessType(
                xClass,
                classLevelAccessType,
                localAttributeMap,
                persistentAttributesFromGetters,
                fields,
                getters
        );
        this.persistentAttributes = verifyAndInitializePersistentAttributes(xClass, localAttributeMap);
    }

    private void preFilter(List<XProperty> fields, List<XProperty> getters) {
        Iterator<XProperty> propertyIterator = fields.iterator();
        while (propertyIterator.hasNext()) {
            final XProperty property = propertyIterator.next();
            if (mustBeSkipped(property)) {
                propertyIterator.remove();
            }
        }

        propertyIterator = getters.iterator();
        while (propertyIterator.hasNext()) {
            final XProperty property = propertyIterator.next();
            if (mustBeSkipped(property)) {
                propertyIterator.remove();
            }
        }
    }

    private static void collectPersistentAttributesUsingLocalAccessType(
            XClass xClass,
            LinkedHashMap<String, XProperty> persistentAttributeMap,
            Map<String, XProperty> persistentAttributesFromGetters,
            List<XProperty> fields,
            List<XProperty> getters) {

        // Check fields...
        Iterator<XProperty> propertyIterator = fields.iterator();
        while (propertyIterator.hasNext()) {
            final XProperty xProperty = propertyIterator.next();
            final Access localAccessAnnotation = xProperty.getAnnotation(Access.class);
            if (localAccessAnnotation == null
                    || localAccessAnnotation.value() != javax.persistence.AccessType.FIELD) {
                continue;
            }

            propertyIterator.remove();
            persistentAttributeMap.put(xProperty.getName(), xProperty);
        }

        // Check getters...
        propertyIterator = getters.iterator();
        while (propertyIterator.hasNext()) {
            final XProperty xProperty = propertyIterator.next();
            final Access localAccessAnnotation = xProperty.getAnnotation(Access.class);
            if (localAccessAnnotation == null
                    || localAccessAnnotation.value() != javax.persistence.AccessType.PROPERTY) {
                continue;
            }

            propertyIterator.remove();

            final String name = xProperty.getName();

            // HHH-10242 detect registration of the same property getter twice - eg boolean isId() + UUID getId()
            final XProperty previous = persistentAttributesFromGetters.get(name);
            if (previous != null) {
                throw new org.hibernate.boot.MappingException(
                        LOG.ambiguousPropertyMethods(
                                xClass.getName(),
                                HCANNHelper.annotatedElementSignature(previous),
                                HCANNHelper.annotatedElementSignature(xProperty)
                        ),
                        new Origin(SourceType.ANNOTATION, xClass.getName())
                );
            }

            persistentAttributeMap.put(name, xProperty);
            persistentAttributesFromGetters.put(name, xProperty);
        }
    }

    private static void collectPersistentAttributesUsingClassLevelAccessType(
            XClass xClass,
            AccessType classLevelAccessType,
            LinkedHashMap<String, XProperty> persistentAttributeMap,
            Map<String, XProperty> persistentAttributesFromGetters,
            List<XProperty> fields,
            List<XProperty> getters) {
        if (classLevelAccessType == AccessType.FIELD) {
            for (XProperty field : fields) {
                if (persistentAttributeMap.containsKey(field.getName())) {
                    continue;
                }

                persistentAttributeMap.put(field.getName(), field);
            }
        } else {
            for (XProperty getter : getters) {
                final String name = getter.getName();

                // HHH-10242 detect registration of the same property getter twice - eg boolean isId() + UUID getId()
                final XProperty previous = persistentAttributesFromGetters.get(name);
                if (previous != null) {
                    throw new org.hibernate.boot.MappingException(
                            LOG.ambiguousPropertyMethods(
                                    xClass.getName(),
                                    HCANNHelper.annotatedElementSignature(previous),
                                    HCANNHelper.annotatedElementSignature(getter)
                            ),
                            new Origin(SourceType.ANNOTATION, xClass.getName())
                    );
                }

                if (persistentAttributeMap.containsKey(name)) {
                    continue;
                }

                persistentAttributeMap.put(getter.getName(), getter);
                persistentAttributesFromGetters.put(name, getter);
            }
        }
    }

    public XClass getEntityAtStake() {
        return entityAtStake;
    }

    public XClass getDeclaringClass() {
        return xClass;
    }

    public AccessType getClassLevelAccessType() {
        return classLevelAccessType;
    }

    /**
     * @deprecated Use the {@link #propertyIterator()} method instead.
     */
    @Deprecated
    public Collection<XProperty> getProperties() {
        return Collections.unmodifiableCollection(this.persistentAttributes);
    }

    public Iterable<XProperty> propertyIterator() {
        return persistentAttributes;
    }

    private static List<XProperty> verifyAndInitializePersistentAttributes(XClass xClass, Map<String, XProperty> localAttributeMap) {
        ArrayList<XProperty> output = new ArrayList(localAttributeMap.size());
        for (XProperty xProperty : localAttributeMap.values()) {
            if (!xProperty.isTypeResolved() && !discoverTypeWithoutReflection(xProperty)) {
                String msg = "Property " + StringHelper.qualify(xClass.getName(), xProperty.getName()) +
                        " has an unbound type and no explicit target entity. Resolve this Generic usage issue" +
                        " or set an explicit target attribute (eg @OneToMany(target=) or use an explicit @Type";
                throw new AnnotationException(msg);
            }
            output.add(xProperty);
        }
        return CollectionHelper.toSmallList(output);
    }

    private AccessType determineLocalClassDefinedAccessStrategy() {
        AccessType classDefinedAccessType;

        AccessType hibernateDefinedAccessType = AccessType.DEFAULT;
        AccessType jpaDefinedAccessType = AccessType.DEFAULT;

        org.hibernate.annotations.AccessType accessType = xClass.getAnnotation(org.hibernate.annotations.AccessType.class);
        if (accessType != null) {
            hibernateDefinedAccessType = AccessType.getAccessStrategy(accessType.value());
        }

        Access access = xClass.getAnnotation(Access.class);
        if (access != null) {
            jpaDefinedAccessType = AccessType.getAccessStrategy(access.value());
        }

        if (hibernateDefinedAccessType != AccessType.DEFAULT
                && jpaDefinedAccessType != AccessType.DEFAULT
                && hibernateDefinedAccessType != jpaDefinedAccessType) {
            throw new MappingException(
                    "@AccessType and @Access specified with contradicting values. Use of @Access only is recommended. "
            );
        }

        if (hibernateDefinedAccessType != AccessType.DEFAULT) {
            classDefinedAccessType = hibernateDefinedAccessType;
        } else {
            classDefinedAccessType = jpaDefinedAccessType;
        }
        return classDefinedAccessType;
    }

    private static boolean discoverTypeWithoutReflection(XProperty p) {
        if (p.isAnnotationPresent(OneToOne.class) && !p.getAnnotation(OneToOne.class)
                .targetEntity()
                .equals(void.class)) {
            return true;
        } else if (p.isAnnotationPresent(OneToMany.class) && !p.getAnnotation(OneToMany.class)
                .targetEntity()
                .equals(void.class)) {
            return true;
        } else if (p.isAnnotationPresent(ManyToOne.class) && !p.getAnnotation(ManyToOne.class)
                .targetEntity()
                .equals(void.class)) {
            return true;
        } else if (p.isAnnotationPresent(ManyToMany.class) && !p.getAnnotation(ManyToMany.class)
                .targetEntity()
                .equals(void.class)) {
            return true;
        } else if (p.isAnnotationPresent(org.hibernate.annotations.Any.class)) {
            return true;
        } else if (p.isAnnotationPresent(ManyToAny.class)) {
            if (!p.isCollection() && !p.isArray()) {
                throw new AnnotationException("@ManyToAny used on a non collection non array property: " + p.getName());
            }
            return true;
        } else if (p.isAnnotationPresent(Type.class)) {
            return true;
        } else return p.isAnnotationPresent(Target.class);
    }

    private static boolean mustBeSkipped(XProperty property) {
        //TODO make those hardcoded tests more portable (through the bytecode provider?)
        return property.isAnnotationPresent(Transient.class)
                || "net.sf.cglib.transform.impl.InterceptFieldCallback".equals(property.getType().getName());
    }
}
