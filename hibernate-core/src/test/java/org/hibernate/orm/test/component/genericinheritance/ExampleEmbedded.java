package org.hibernate.orm.test.component.genericinheritance;

import jakarta.persistence.Embeddable;

@Embeddable
public class ExampleEmbedded<T> extends ExampleSuperClassEmbedded<T> {
}
