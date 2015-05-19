/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.component.basic2;


import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * {@inheritDoc}
 *
 * @author Mathieu Grenonville
 */
@Entity
public class Component {

    @Id
	private Long id;
    
    @Embedded
	private Component.Emb emb;

    @Access(AccessType.FIELD)
    @Embeddable
    public static class Emb {

        @OneToMany(targetEntity = Stuff.class)
        Set<Stuff> stuffs = new HashSet<Stuff>();

        @Entity
        public static class Stuff {
            @Id
            private Long id;
        }
    }


}
