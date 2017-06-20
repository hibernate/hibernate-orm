/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.inheritance.polymorphism;

/**
 * @author Vlad Mihalcea
 */
//tag::entity-inheritance-polymorphism-interface-example[]
public interface DomainModelEntity<ID> {

    ID getId();

    Integer getVersion();
}
//end::entity-inheritance-polymorphism-interface-example[]
