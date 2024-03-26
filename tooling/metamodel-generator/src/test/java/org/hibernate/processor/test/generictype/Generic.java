package org.hibernate.processor.test.generictype;

import jakarta.persistence.*;
import org.hibernate.annotations.ManyToAny;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
public class Generic<T extends Number> {
    @Id Long id;
    @ManyToOne Generic<T> parent;
    @OneToMany Set<Generic<T>> children;
    @ElementCollection List<T> list;
    @ElementCollection Map<T,T> map;
    @ManyToAny Set<T> set;
}
