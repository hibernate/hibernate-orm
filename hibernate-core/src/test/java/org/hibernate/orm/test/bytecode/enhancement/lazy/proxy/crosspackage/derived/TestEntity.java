/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.crosspackage.derived;

import jakarta.persistence.Entity;

import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.crosspackage.base.BaseEntity;

@Entity
public class TestEntity extends BaseEntity {

}
