package org.hibernate.orm.test.entitygraph;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.Map;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Jpa(
    annotatedClasses = {
      EntityGraphEmbeddableTest.TrackedProduct.class,
      EntityGraphEmbeddableTest.Tracking.class,
      EntityGraphEmbeddableTest.UserForTracking.class,
    })
@JiraKey("HHH-18391")
class EntityGraphEmbeddableTest {

  private static final int TRACKED_PRODUCT_ID = 1;

  @BeforeEach
  public void beforeEach(EntityManagerFactoryScope scope) {
    scope.inTransaction(
        entityManager -> {
          EntityGraphEmbeddableTest.UserForTracking creator =
              new EntityGraphEmbeddableTest.UserForTracking(1, "foo");
          entityManager.persist(creator);

          EntityGraphEmbeddableTest.UserForTracking modifier =
              new EntityGraphEmbeddableTest.UserForTracking(2, "bar");
          entityManager.persist(modifier);

          EntityGraphEmbeddableTest.Tracking tracking =
              new EntityGraphEmbeddableTest.Tracking(creator, modifier);

          EntityGraphEmbeddableTest.TrackedProduct product =
              new EntityGraphEmbeddableTest.TrackedProduct(
                  TRACKED_PRODUCT_ID, UUID.randomUUID().toString(), tracking);
          entityManager.persist(product);
        });
  }

  @Test
  @DisplayName("Entity fetch graph is well applied on embedded entities")
  void testFetchGraph(EntityManagerFactoryScope scope) {
    scope.inTransaction(
        entityManager -> {
          EntityGraph<TrackedProduct> trackedProductGraph =
              entityManager.createEntityGraph(TrackedProduct.class);
          trackedProductGraph.addSubgraph("tracking").addAttributeNodes("creator");

          TrackedProduct product =
              entityManager.find(
                  TrackedProduct.class,
                  TRACKED_PRODUCT_ID,
                  Map.of("javax.persistence.fetchgraph", trackedProductGraph));

          assertThat(Hibernate.isInitialized(product.tracking.creator)).isTrue();
          assertThat(Hibernate.isInitialized(product.tracking.modifier)).isFalse();
        });
  }

  @Test
  @DisplayName("Entity load graph is well applied on embedded entities")
  void testLoadGraph(EntityManagerFactoryScope scope) {
    scope.inTransaction(
        entityManager -> {
          EntityGraph<TrackedProduct> trackedProductGraph =
              entityManager.createEntityGraph(TrackedProduct.class);
          trackedProductGraph.addSubgraph("tracking").addAttributeNodes("creator");

          TrackedProduct product =
              entityManager.find(
                  TrackedProduct.class,
                  TRACKED_PRODUCT_ID,
                  Map.of("javax.persistence.loadgraph", trackedProductGraph));

          assertThat(Hibernate.isInitialized(product.tracking.creator)).isTrue();
          assertThat(Hibernate.isInitialized(product.tracking.modifier)).isFalse();
        });
  }

  @Entity(name = "TrackedProduct")
  public static class TrackedProduct {
    @Id private Integer id;

    @Embedded private Tracking tracking;

    private String barcode;

    public TrackedProduct() {}

    public TrackedProduct(Integer id, String barcode, Tracking tracking) {
      this.id = id;
      this.barcode = barcode;
      this.tracking = tracking;
    }
  }

  @Embeddable
  public static class Tracking {
    @ManyToOne(fetch = FetchType.LAZY)
    private UserForTracking creator;

    @ManyToOne(fetch = FetchType.LAZY)
    private UserForTracking modifier;

    public Tracking() {}

    public Tracking(UserForTracking creator, UserForTracking modifier) {
      this.creator = creator;
      this.modifier = modifier;
    }
  }

  @Entity(name = "UserForTracking")
  public static class UserForTracking {
    @Id private Integer id;

    private String login;

    public UserForTracking() {}

    public UserForTracking(Integer id, String login) {
      this.id = id;
      this.login = login;
    }
  }
}
