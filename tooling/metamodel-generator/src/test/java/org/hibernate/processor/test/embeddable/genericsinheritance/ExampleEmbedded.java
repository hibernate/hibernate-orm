package org.hibernate.processor.test.embeddable.genericsinheritance;

import jakarta.persistence.Embeddable;

@Embeddable
public class ExampleEmbedded<T> extends ExampleSuperClassEmbedded<T> {
}
