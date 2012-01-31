package org.hibernate.envers.entities.mapper.relation.query;

/**
 * Constants used in JPQL queries.
 * 
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class QueryConstants {
    public static final String REFERENCED_ENTITY_ALIAS = "e_h";
    public static final String REFERENCED_ENTITY_ALIAS_DEF_AUD_STR = "e2_h";

    public static final String INDEX_ENTITY_ALIAS = "f_h";
    public static final String INDEX_ENTITY_ALIAS_DEF_AUD_STR = "f2_h";

    public static final String MIDDLE_ENTITY_ALIAS = "ee_h";
    public static final String MIDDLE_ENTITY_ALIAS_DEF_AUD_STR = "ee2_h";

    public static final String REVISION_PARAMETER = "revision";
    public static final String DEL_REVISION_TYPE_PARAMETER = "delrevisiontype";
}
