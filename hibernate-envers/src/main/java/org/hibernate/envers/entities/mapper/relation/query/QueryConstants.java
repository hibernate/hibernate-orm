package org.hibernate.envers.entities.mapper.relation.query;

/**
 * Constants used in JPQL queries.
 * 
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class QueryConstants {
    public static final String REFERENCED_ENTITY_ALIAS = "e__";
    public static final String REFERENCED_ENTITY_ALIAS_DEF_AUD_STR = "e2__";

    public static final String INDEX_ENTITY_ALIAS = "f__";
    public static final String INDEX_ENTITY_ALIAS_DEF_AUD_STR = "f2__";

    public static final String MIDDLE_ENTITY_ALIAS = "ee__";
    public static final String MIDDLE_ENTITY_ALIAS_DEF_AUD_STR = "ee2__";

    public static final String REVISION_PARAMETER = "revision";
    public static final String DEL_REVISION_TYPE_PARAMETER = "delrevisiontype";
}
