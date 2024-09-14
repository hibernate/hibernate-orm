/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.loading.multiLoad;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.EnabledFetchProfile;
import org.hibernate.Hibernate;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = {FindAllFetchProfileTest.Record.class, FindAllFetchProfileTest.Owner.class})
public class FindAllFetchProfileTest {
    @Test void test(SessionFactoryScope scope) {
        scope.inTransaction(s-> {
            Owner gavin = new Owner("gavin");
            s.persist(gavin);
            s.persist(new Record(123L,gavin,"hello earth"));
            s.persist(new Record(456L,gavin,"hello mars"));
        });
        scope.inTransaction(s-> {
            List<Record> all = s.findAll(Record.class, List.of(456L, 123L, 2L));
            assertEquals("hello mars",all.get(0).message);
            assertEquals("hello earth",all.get(1).message);
            assertNull(all.get(2));
            assertFalse(Hibernate.isInitialized(all.get(0).owner));
            assertFalse(Hibernate.isInitialized(all.get(1).owner));
        });
        scope.inTransaction(s-> {
            List<Record> all = s.findAll(Record.class, List.of(456L, 123L),
                    new EnabledFetchProfile("withOwner"));
            assertEquals("hello mars",all.get(0).message);
            assertEquals("hello earth",all.get(1).message);
            assertTrue(Hibernate.isInitialized(all.get(0).owner));
            assertTrue(Hibernate.isInitialized(all.get(1).owner));
        });
    }
    @Entity
    @FetchProfile(name = "withOwner")
    static class Record {
        @Id Long id;
        String message;

        @FetchProfileOverride(profile = "withOwner")
        @ManyToOne(fetch = FetchType.LAZY)
        Owner owner;

        Record(Long id, Owner owner, String message) {
            this.id = id;
            this.owner = owner;
            this.message = message;
        }

        Record() {
        }
    }
    @Entity
    static class Owner {
        @Id String name;

        Owner(String name) {
            this.name = name;
        }

        Owner() {
        }
    }
}
