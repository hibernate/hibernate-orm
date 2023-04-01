package org.hibernate.orm.test.hql;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@DomainModel(
        annotatedClasses = {
                SubqueryWithCriteriaQueryTest.MyUnrelatedEntity.class,
                SubqueryWithCriteriaQueryTest.MyEntity.class,
                SubqueryWithCriteriaQueryTest.AnotherEntity.class,
                SubqueryWithCriteriaQueryTest.AgainAnotherEntity.class
        }
)
@SessionFactory
@TestForIssue(jiraKey = "HHH-16413")
public class SubqueryWithCriteriaQueryTest {

    private static final Long ENTITY_WITH_ASSOCIATION_ID_1 = 1L;
    private static final Long ENTITY_WITH_ASSOCIATION_ID_2 = 2L;
    private static final long ANOTHER_ENTITY_ID_1 = 3l;
    private static final long ANOTHER_ENTITY_ID_2 = 4l;
    private static final long AGAIN_ANOTHER_ENTITY_ID_1 = 5l;
    private static final long AGAIN_ANOTHER_ENTITY_ID_2 = 6l;

    @BeforeAll
    public void setUp(SessionFactoryScope scope) {
        scope.inTransaction(
                session -> {
                    AgainAnotherEntity againAnotherEntity1 = new AgainAnotherEntity(AGAIN_ANOTHER_ENTITY_ID_1, "again1");
                    session.persist(againAnotherEntity1);

                    AgainAnotherEntity againAnotherEntity2 = new AgainAnotherEntity(AGAIN_ANOTHER_ENTITY_ID_2, "again2");
                    session.persist(againAnotherEntity2);

                    AnotherEntity anotherEntity1 = new AnotherEntity(ANOTHER_ENTITY_ID_1, "another 1", true, againAnotherEntity1);
                    session.persist(anotherEntity1);

                    AnotherEntity anotherEntity2 = new AnotherEntity(ANOTHER_ENTITY_ID_2, "another 2", false, againAnotherEntity2);
                    session.persist(anotherEntity2);

                    MyEntity entity = new MyEntity(ENTITY_WITH_ASSOCIATION_ID_1, "with association 1", anotherEntity1);
                    session.persist(entity);

                    MyEntity entity2 = new MyEntity(ENTITY_WITH_ASSOCIATION_ID_2, "with assosiation 2", anotherEntity2);
                    session.persist(entity2);

                    MyUnrelatedEntity myUnrelatedEntity1 = new MyUnrelatedEntity(ENTITY_WITH_ASSOCIATION_ID_1, "unrelated 1");
                    session.persist(myUnrelatedEntity1);

                    MyUnrelatedEntity myUnrelatedEntity2 = new MyUnrelatedEntity(ENTITY_WITH_ASSOCIATION_ID_2, "unrelated 2");
                    session.persist(myUnrelatedEntity2);
                }
        );
    }

    @Test
    public void subqueryWithLeftJoinsCriteriaApi(SessionFactoryScope sessionFactoryScope) {
        sessionFactoryScope.inTransaction(session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<MyUnrelatedEntity> cq = cb.createQuery(MyUnrelatedEntity.class);
            Root<MyUnrelatedEntity> root = cq.from(MyUnrelatedEntity.class);

            Subquery<Long> subquery = cq.subquery(Long.class);
            Root<MyEntity> myEntityRoot = subquery.from(MyEntity.class);
            Join<MyEntity, AnotherEntity> myEntityAnotherEntityJoin = myEntityRoot.join("otherEntity", JoinType.LEFT);
            Join<AnotherEntity, AgainAnotherEntity> anotherEntityAgainAnotherEntityJoin = myEntityAnotherEntityJoin.join("otherEntity", JoinType.LEFT);

            subquery.select(myEntityRoot.get("id"))
                    .where(cb.and(
                                   cb.equal(
                                           myEntityAnotherEntityJoin.get("aString"), "another 1"),
                                   cb.or(
                                           cb.and(
                                                   cb.equal(myEntityAnotherEntityJoin.get("aBoolean"), false),
                                                   cb.equal(anotherEntityAgainAnotherEntityJoin.get("aString"), "again2")
                                           ),
                                           cb.and(
                                                   cb.equal(myEntityAnotherEntityJoin.get("aBoolean"), true),
                                                   cb.equal(anotherEntityAgainAnotherEntityJoin.get("aString"), "again1")
                                           )
                                   )
                           )
                    );
            session.createQuery(cq.select(root)
                                  .where(root.get("id")
                                             .in(subquery)))
                   .getResultList();

            List<MyUnrelatedEntity> results = session.createQuery(cq.select(root)
                                                                    .where(root.get("id")
                                                                               .in(subquery)))
                                                     .list();
            assertThat(results.size()).isEqualTo(2);
        });
    }

    @Entity(name = "MyUnrelatedEntity")
    public static class MyUnrelatedEntity {

        @Id
        private Long id;

        private String aString;

        public MyUnrelatedEntity() {
        }

        public MyUnrelatedEntity(Long id, String aString) {
            this.id = id;
            this.aString = aString;
        }

        public Long getId() {
            return id;
        }

        public String getaString() {
            return aString;
        }
    }

    @Entity(name = "MyEntity")
    public static class MyEntity {

        @Id
        private Long id;

        private String aString;

        @OneToMany
        private List<AnotherEntity> anotherEntities;

        @ManyToOne
        private AnotherEntity otherEntity;

        public MyEntity() {
        }

        public MyEntity(Long id, String aString, AnotherEntity otherEntity) {
            this.id = id;
            this.aString = aString;
            this.otherEntity = otherEntity;
        }

        public Long getId() {
            return id;
        }

        public String getaString() {
            return aString;
        }

        public AnotherEntity getOtherEntity() {
            return otherEntity;
        }
    }

    @Entity(name = "AnotherEntity")
    public static class AnotherEntity {
        @Id
        private Long id;

        private String aString;

        private boolean aBoolean;
        @ManyToOne
        private AgainAnotherEntity otherEntity;

        public AnotherEntity() {
        }

        public AnotherEntity(Long id, String aString, boolean aBoolean, AgainAnotherEntity otherEntity) {
            this.id = id;
            this.aString = aString;
            this.otherEntity = otherEntity;
            this.aBoolean = aBoolean;
        }

        public String getaString() {
            return aString;
        }
    }

    @Entity(name = "AgainAnotherEntity")
    public static class AgainAnotherEntity {
        @Id
        private Long id;

        private String aString;

        @ManyToOne
        private AnotherEntity otherEntity;

        public AgainAnotherEntity() {
        }

        public AgainAnotherEntity(Long id, String aString) {
            this.id = id;
            this.aString = aString;
        }

    }


}
