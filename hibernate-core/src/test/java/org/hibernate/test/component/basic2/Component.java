package org.hibernate.test.component.basic2;


import java.util.HashSet;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

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
