/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.unionsubclass3;

import javax.persistence.*;

/**
 * @author pholvs
 */
@Entity
public class ClassOfInterest
{
    @Id
    Long id;

    @ManyToOne
    OtherClassIface otherEntity;

}
