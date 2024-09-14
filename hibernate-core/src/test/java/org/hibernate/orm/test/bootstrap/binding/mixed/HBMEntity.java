/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.mixed;

public class HBMEntity {

    private long _id;
    private AnnotationEntity _association;

    public long getId() {
        return _id;
    }

    public void setId(long id) {
        _id = id;
    }

    public AnnotationEntity getAssociation() {
        return _association;
    }

    public void setAssociation(AnnotationEntity association) {
        _association = association;
    }
}
