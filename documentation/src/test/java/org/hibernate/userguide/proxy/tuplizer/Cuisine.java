/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.userguide.proxy.tuplizer;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Tuplizer;

/**
 * @author Emmanuel Bernard
 */
//tag::entity-tuplizer-entity-mapping[]
@Entity
@Tuplizer(impl = DynamicEntityTuplizer.class)
public interface Cuisine {

    @Id
    @GeneratedValue
    Long getId();
    void setId(Long id);

    String getName();
    void setName(String name);

    @Tuplizer(impl = DynamicEmbeddableTuplizer.class)
    Country getCountry();
    void setCountry(Country country);
}
//end::entity-tuplizer-entity-mapping[]
