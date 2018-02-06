/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.joinedsubclassduplicatefields;

import org.hibernate.test.joinedsubclassduplicatefields.MySuperClass;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by pholvenstot on 11/21/2017.
 */
@Entity
public class SubClassA extends MySuperClass {
    @Column
    public String fieldOnChild;
}
