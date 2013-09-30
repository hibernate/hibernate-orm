package org.hibernate.envers.test.integration.components.dynamic;

import junit.framework.Assert;
import org.hibernate.Session;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

public class AuditedDynamicComponentsAdvancedCasesTest extends BaseEnversFunctionalTestCase {

    public static final String PROP_BOOLEAN = "propBoolean";
    public static final String PROP_INT = "propInt";
    public static final String PROP_FLOAT = "propFloat";
    public static final String PROP_MANY_TO_ONE = "propManyToOne";
    public static final String PROP_ONE_TO_ONE = "propOneToOne";

    @Override
    protected String[] getMappings() {
        return new String[]{"mappings/dynamicComponents/mapAdvanced.hbm.xml"};
    }

    @Test
    @Priority(10)
    //smoke test to make sure that hibernate & envers are working with the entity&mappings
    public void shouldSaveEntity() {
        //given
        ManyToOneEntity manyToOne = getManyToOneEntity();
        OneToOneEntity oneToOne = getOneToOneEntity();
        AdvancedEntity advancedEntity = getAdvancedEntity(manyToOne, oneToOne);


        Session session = openSession();
        session.getTransaction().begin();
        session.save(manyToOne);
        session.save(oneToOne);
        session.save(advancedEntity);
        session.getTransaction().commit();

        AdvancedEntity advancedEntityActual = (AdvancedEntity) session.load(AdvancedEntity.class, 1L);

        Assert.assertEquals(advancedEntity, advancedEntityActual);
    }

    private AdvancedEntity getAdvancedEntity(ManyToOneEntity manyToOne, OneToOneEntity oneToOne) {
        AdvancedEntity advancedEntity = new AdvancedEntity();
        advancedEntity.setId(1L);
        advancedEntity.setNote("Test note");
        advancedEntity.getDynamicConfiguration().put(PROP_BOOLEAN, true);
        advancedEntity.getDynamicConfiguration().put(PROP_INT, 19);
        advancedEntity.getDynamicConfiguration().put(PROP_FLOAT, 15.9f);
        advancedEntity.getDynamicConfiguration().put(PROP_MANY_TO_ONE, manyToOne);
        advancedEntity.getDynamicConfiguration().put(PROP_ONE_TO_ONE, oneToOne);
        return advancedEntity;
    }

    @Test
    public void shouldMakeFirstRevision() {
        Session session = openSession();
        //given & when shouldSaveEntity
        ManyToOneEntity manyToOne = getManyToOneEntity();
        OneToOneEntity oneToOne = getOneToOneEntity();
        AdvancedEntity advancedEntity = getAdvancedEntity(manyToOne, oneToOne);

        //then
        session.getTransaction().begin();
        AdvancedEntity ver1 = getAuditReader().find(
                AdvancedEntity.class,
                advancedEntity.getId(),
                1
        );
        Assert.assertEquals(advancedEntity, ver1);
        session.getTransaction().commit();
    }

    private OneToOneEntity getOneToOneEntity() {
        return new OneToOneEntity(1L, "OneToOne");
    }

    private ManyToOneEntity getManyToOneEntity() {
        return new ManyToOneEntity(1L, "ManyToOne");
    }

}
