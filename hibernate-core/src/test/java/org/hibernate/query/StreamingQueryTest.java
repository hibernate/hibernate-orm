package org.hibernate.query;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

public class StreamingQueryTest extends BaseEntityManagerFunctionalTestCase {

    private static final int SIZE = 20;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Blah.class};
    }

    @Override
    protected void afterEntityManagerFactoryBuilt() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            IntStream
                    .rangeClosed(1, SIZE)
                    .mapToObj(i -> new Blah(i, "name-" + i))
                    .forEach(entityManager::persist);
        });
    }

    @Test
    public void testOnCloseCalled() {
        doInJPA(this::entityManagerFactory, entityManager -> {

            Stream<Blah> stream = getBlahStream(entityManager);

            AtomicInteger timesOnCloseCalled = new AtomicInteger();

            Stream<Blah> withOnclose = stream.onClose(timesOnCloseCalled::incrementAndGet);

            assertThat(timesOnCloseCalled).hasValue(0);

            try (withOnclose) {
                assertThat(withOnclose.collect(Collectors.toList())).hasSize(SIZE);
            }

            assertThat(timesOnCloseCalled).hasValue(1);
        });
    }

    @Test
    public void testOnCloseCalledWhenFlatMapIsUsedBeforeOnClose() {
        doInJPA(this::entityManagerFactory, entityManager -> {

            Stream<Blah> stream = getBlahStream(entityManager);

            AtomicInteger timesOnCloseCalled = new AtomicInteger();

            Stream<Blah> withOnclose = stream.flatMap(Stream::of).onClose(timesOnCloseCalled::incrementAndGet);

            assertThat(timesOnCloseCalled).hasValue(0);

            try (withOnclose) {
                assertThat(withOnclose.collect(Collectors.toList())).hasSize(SIZE);
            }

            assertThat(timesOnCloseCalled).hasValue(1);
        });
    }

    @Test
    public void testOnCloseCalledWhenFlatMapIsUsedAfterOnClose() {
        doInJPA(this::entityManagerFactory, entityManager -> {

            Stream<Blah> stream = getBlahStream(entityManager);

            AtomicInteger timesOnCloseCalled = new AtomicInteger();

            Stream<Blah> withOnclose = stream.onClose(timesOnCloseCalled::incrementAndGet).flatMap(Stream::of);

            assertThat(timesOnCloseCalled).hasValue(0);

            try (withOnclose) {
                assertThat(withOnclose.collect(Collectors.toList())).hasSize(SIZE);
            }

            assertThat(timesOnCloseCalled).hasValue(1);
        });
    }

    @SuppressWarnings("unchecked")
    private static Stream<Blah> getBlahStream(EntityManager entityManager) {
        return entityManager.createQuery("SELECT b FROM Blah b").getResultStream();
    }

    @Entity(name = "Blah")
    public static class Blah {
        @Id
        int id;
        private String name;

        public Blah() {
        }

        public Blah(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
