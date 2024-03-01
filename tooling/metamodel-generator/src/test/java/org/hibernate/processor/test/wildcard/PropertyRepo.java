package org.hibernate.processor.test.wildcard;

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.ManyToAny;

import java.util.ArrayList;
import java.util.List;

@Entity
public class PropertyRepo {
    @Id
    private Long id;

    @ManyToAny
    @AnyDiscriminator(DiscriminatorType.STRING)
    private List<Property<?>> properties = new ArrayList<>();
}
