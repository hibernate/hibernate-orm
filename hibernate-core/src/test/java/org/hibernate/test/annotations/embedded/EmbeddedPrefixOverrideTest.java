/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embedded;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.test.annotations.embedded.EntityWithEmbeddedPrefixOverride.EmbeddableExample;
import org.hibernate.test.annotations.embedded.EntityWithEmbeddedPrefixOverride.NestedEmbeddableExample;
import org.hibernate.test.util.SchemaUtil;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Kevin Dargel
 */
public class EmbeddedPrefixOverrideTest extends BaseNonConfigCoreFunctionalTestCase {

  @Test
  public void testEmbeddedPrefixOverrideColumns() {
    assertTrue(SchemaUtil.isColumnPresent("TableEmbeddedPrefixOverride", "overridden_someString", metadata()));
    assertTrue(SchemaUtil.isColumnPresent("TableEmbeddedPrefixOverride", "embedNotOverridden_someString", metadata()));
    assertTrue(SchemaUtil.isColumnPresent("TableEmbeddedPrefixOverride", "nestedOverriddenColumn", metadata()));
    assertTrue(SchemaUtil.isColumnPresent("TableEmbeddedPrefixOverride", "nestedEmbedNotOverridden_overriddenEmbedNested_someString", metadata()));
  }

  @Test
  public void testEmbeddedPrefixOverridePersist() {
    var entity = new EntityWithEmbeddedPrefixOverride();
    var embedExample1 = new EmbeddableExample();
    embedExample1.setSomeString("ABC");
    entity.setEmbed(embedExample1);
    var embedExample2 = new EmbeddableExample();
    embedExample2.setSomeString("DEF");
    entity.setEmbedNotOverridden(embedExample2);

    var nestedEmbedExample = new NestedEmbeddableExample();
    var embedExample3 = new EmbeddableExample();
    embedExample3.setSomeString("GHI");
    nestedEmbedExample.setEmbed(embedExample3);
    entity.setNestedEmbed(nestedEmbedExample);

    var nestedEmbedExampleOverridden = new NestedEmbeddableExample();
    var embedExample4 = new EmbeddableExample();
    embedExample4.setSomeString("JKL");
    nestedEmbedExampleOverridden.setEmbedNotOverridden(embedExample4);
    entity.setNestedEmbedNotOverridden(nestedEmbedExampleOverridden);

    Session s = openSession();
    s.beginTransaction();
    s.persist(entity);
    s.getTransaction().commit();

    List<EntityWithEmbeddedPrefixOverride> result = s.createQuery("""
                                                             select t
                                                             from EntityWithEmbeddedPrefixOverride t
                                                             where t.embed.someString = 'ABC'
                                                             AND t.nestedEmbed.embed.someString = 'GHI'
                                                             AND t.nestedEmbedNotOverridden.embedNotOverridden.someString = 'JKL'
                                                         """, EntityWithEmbeddedPrefixOverride.class)
                                                     .getResultList();

    assertEquals(1, result.size());
    assertEquals(result.get(0).getEmbed().getSomeString(), "ABC");
    assertEquals(result.get(0).getNestedEmbed().getEmbed().getSomeString(), "GHI");
    assertEquals(result.get(0).getNestedEmbedNotOverridden().getEmbedNotOverridden().getSomeString(), "JKL");
    s.close();
  }

  @Override
  protected void initialize(MetadataBuilder metadataBuilder) {
    super.initialize(metadataBuilder);
    metadataBuilder.applyImplicitNamingStrategy(
        ImplicitNamingStrategyComponentPathImpl.INSTANCE
    );
  }

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class<?>[]{EntityWithEmbeddedPrefixOverride.class};
  }
}
