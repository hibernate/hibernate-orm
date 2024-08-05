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
      EntityGraphEmbeddedAttributesTest.TrackedProduct.class,
      EntityGraphEmbeddedAttributesTest.Tracking.class,
      EntityGraphEmbeddedAttributesTest.UserForTracking.class,
    })
@JiraKey("HHH-18391")
class EntityGraphEmbeddedAttributesTest {

  private static final int TRACKED_PRODUCT_ID = 1;

  @BeforeEach
  public void beforeEach(EntityManagerFactoryScope scope) {
    scope.inTransaction(
        entityManager -> {
          EntityGraphEmbeddedAttributesTest.UserForTracking creator =
              new EntityGraphEmbeddedAttributesTest.UserForTracking(1, "foo");
          entityManager.persist(creator);

          EntityGraphEmbeddedAttributesTest.UserForTracking modifier =
              new EntityGraphEmbeddedAttributesTest.UserForTracking(2, "bar");
          entityManager.persist(modifier);

          EntityGraphEmbeddedAttributesTest.Tracking tracking =
              new EntityGraphEmbeddedAttributesTest.Tracking(creator, modifier);

          EntityGraphEmbeddedAttributesTest.TrackedProduct product =
              new EntityGraphEmbeddedAttributesTest.TrackedProduct(
                  TRACKED_PRODUCT_ID, UUID.randomUUID().toString(), tracking);
          entityManager.persist(product);
        });
  }

  @Test
  @DisplayName("Entity fetch graph is well applied on Embedded attributes")
  void testFetchGraph(EntityManagerFactoryScope scope) {
    scope.inTransaction(
        entityManager -> {
          EntityGraph<EntityGraphEmbeddedAttributesTest.TrackedProduct> trackedProductGraph =
              entityManager.createEntityGraph(
                  EntityGraphEmbeddedAttributesTest.TrackedProduct.class);
          trackedProductGraph.addSubgraph("tracking").addAttributeNodes("creator");

          EntityGraphEmbeddedAttributesTest.TrackedProduct product =
              entityManager.find(
                  EntityGraphEmbeddedAttributesTest.TrackedProduct.class,
                  TRACKED_PRODUCT_ID,
                  Map.of("javax.persistence.fetchgraph", trackedProductGraph));

          assertThat(Hibernate.isInitialized(product.tracking.creator)).isTrue();
          assertThat(Hibernate.isInitialized(product.tracking.modifier)).isFalse();
        });
  }

  @Test
  @DisplayName("Entity load graph is well applied on Embedded attributes")
  void testLoadGraph(EntityManagerFactoryScope scope) {
    scope.inTransaction(
        entityManager -> {
          EntityGraph<EntityGraphEmbeddedAttributesTest.TrackedProduct> trackedProductGraph =
              entityManager.createEntityGraph(
                  EntityGraphEmbeddedAttributesTest.TrackedProduct.class);
          trackedProductGraph.addSubgraph("tracking").addAttributeNodes("creator");

          EntityGraphEmbeddedAttributesTest.TrackedProduct product =
              entityManager.find(
                  EntityGraphEmbeddedAttributesTest.TrackedProduct.class,
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
