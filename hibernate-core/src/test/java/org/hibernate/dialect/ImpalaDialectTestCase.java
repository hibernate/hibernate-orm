package org.hibernate.dialect;

import java.sql.Types;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

public class ImpalaDialectTestCase {
    private final ImpalaDialect impalaDialect = new ImpalaDialect();

    @Test
    @TestForIssue(jiraKey = "HHH-14462")
    public void testGetTypeName() {
        String actual = impalaDialect.getTypeName(Types.BIGINT);
        Assert.assertEquals("bigint", actual);
    }

    @Test
    @TestForIssue(jiraKey = "HHH-14462")
    public void testGetSelectClauseNullString() {
        String actualBigInt = impalaDialect.getSelectClauseNullString(Types.BIGINT);
        Assert.assertEquals("cast(null as bigint)", actualBigInt);

        String actualBoolean = impalaDialect.getSelectClauseNullString(Types.BOOLEAN);
        Assert.assertEquals("cast(null as boolean)", actualBoolean);
    }

    @Test
    @TestForIssue(jiraKey = "HHH-14462")
    public void testGetCurrentTimestampSQLFunctionName() {
        String actualBoolean = impalaDialect.getCurrentTimestampSQLFunctionName();
        Assert.assertEquals("current_timestamp", actualBoolean);
    }

    @Test
    @TestForIssue(jiraKey = "HHH-14462")
    public void testGetCurrentTimestampSelectString() {
        String actualBoolean = impalaDialect.getCurrentTimestampSelectString();
        Assert.assertEquals("select current_timestamp()", actualBoolean);
    }
}
