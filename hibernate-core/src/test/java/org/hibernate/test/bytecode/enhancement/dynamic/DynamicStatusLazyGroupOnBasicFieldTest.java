package org.hibernate.test.bytecode.enhancement.dynamic;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.*;
import java.io.Serializable;

import static org.junit.Assert.assertTrue;

@TestForIssue(jiraKey = "HHH-15186")
@RunWith(BytecodeEnhancerRunner.class)
public class DynamicStatusLazyGroupOnBasicFieldTest extends BaseEntityManagerFunctionalTestCase {

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{ FooEntity.class };
    }

    @Test
    public void test() {
        final EntityPersister persister = entityManagerFactory().getMetamodel().entityPersister( FooEntity.class.getName() );
        final EntityMetamodel entityMetamodel = persister.getEntityMetamodel();
        assertTrue(entityMetamodel.isDynamicUpdate());
    }

    // --- //

    @MappedSuperclass
    public static abstract class AbstractEntity<T extends Serializable> {

        public abstract T getId();

        public abstract void setId(T id);

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != getClass()) return false;

            final AbstractEntity<?> other = (AbstractEntity<?>) obj;
            return getId() != null && getId().equals(other.getId());
        }
    }

    @Entity(name = "FooEntity")
    public static class FooEntity extends AbstractEntity<Long> {

        @Id
        private long id;

        private String name;

        @Basic(fetch = FetchType.LAZY)
        @LazyGroup("group1")
        private String surname;

        public static FooEntity of(long id, String name, String surname) {
            final FooEntity f = new FooEntity();
            f.id = id;
            f.name = name;
            f.surname = surname;
            return f;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }

        @Override
        public String toString() {
            return String.format("FooEntity: id=%d, name=%s, surname=%s", id, name, surname);
        }
    }
}

