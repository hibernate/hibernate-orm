/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.userguide.proxy.tuplizer;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
//tag::entity-tuplizer-entity-mapping[]

@Embeddable
public interface Country {

    @Column(name = "CountryName")
    String getName();

    void setName(String name);
}
//end::entity-tuplizer-entity-mapping[]
