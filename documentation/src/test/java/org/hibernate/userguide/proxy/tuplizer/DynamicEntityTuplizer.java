/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.proxy.tuplizer;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.PojoEntityTuplizer;

/**
 * @author Emmanuel Bernard
 */
//tag::entity-tuplizer-instantiator[]
public class DynamicEntityTuplizer extends PojoEntityTuplizer {

    public DynamicEntityTuplizer(
            EntityMetamodel entityMetamodel,
            PersistentClass mappedEntity) {
        super( entityMetamodel, mappedEntity );
    }

    @Override
    protected Instantiator buildInstantiator(
            EntityMetamodel entityMetamodel,
            PersistentClass persistentClass) {
        return new DynamicInstantiator(
            persistentClass.getClassName()
        );
    }

    @Override
    protected ProxyFactory buildProxyFactory(
            PersistentClass persistentClass,
            Getter idGetter,
            Setter idSetter) {
        return super.buildProxyFactory(
            persistentClass, idGetter,
            idSetter
        );
    }
}
//end::entity-tuplizer-instantiator[]
