package org.hibernate.envers.test.integration.collection.norevision;

import org.hibernate.MappingException;
import org.testng.annotations.BeforeMethod;

import java.net.URISyntaxException;

public class CollectionChangeRevisionTest extends CollectionChangeNoRevisionTest {

    protected static final int PERSON_COUNT_NEW_REVISION_ON_COLLECTION = 2;
    protected static final String NEW_REVISION_ON_COLLECTION = "true";

    @Override
    protected void initMappings() throws MappingException, URISyntaxException {
        super.initMappings();
    }

    @Override
    protected int getExpectedPersonRevisionCount() {
        return PERSON_COUNT_NEW_REVISION_ON_COLLECTION;
    }

    @Override
    protected String getCollectionChangeValue() {
        return NEW_REVISION_ON_COLLECTION;
    }
}
