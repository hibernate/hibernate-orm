/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.length;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
public class WithLongTypeStrings {
    @Id
    @GeneratedValue
    public int id;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    public String longish;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    public String long32;
}
