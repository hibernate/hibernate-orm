package org.hibernate.boot.model.source.internal.hbm;

import javax.persistence.*;

@Entity
public class AnnotationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "annotationentity_id_seq")
    @SequenceGenerator(
            name = "annotationentity_id_seq",
            sequenceName = "annotationentity_id_seq"
    )
    private Long _id;

    /**
     * Get the identifier.
     *
     * @return the id.
     */
    public Long getId()
    {
        return _id;
    }
}
