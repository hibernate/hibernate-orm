/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.userguide.proxy.tuplizer;
import org.hibernate.mapping.Component;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.component.PojoComponentTuplizer;

/**
 * @author Emmanuel Bernard
 */
//tag::entity-tuplizer-instantiator[]

public class DynamicEmbeddableTuplizer
        extends PojoComponentTuplizer {

    public DynamicEmbeddableTuplizer(Component embeddable) {
        super( embeddable );
    }

    protected Instantiator buildInstantiator(Component embeddable) {
        return new DynamicInstantiator(
            embeddable.getComponentClassName()
        );
    }
}
//end::entity-tuplizer-instantiator[]
