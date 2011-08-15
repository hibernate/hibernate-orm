package org.hibernate.envers.test;

import org.hibernate.envers.DefaultRevisionEntity;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An abstract Envers test which runs the tests using two audit strategies.
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@RunWith(EnversRunner.class)
public abstract class AbstractEnversTest {
    private String auditStrategy;

    @Parameterized.Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[]{null}, new Object[]{"org.hibernate.envers.strategy.ValidityAuditStrategy"});
    }

    public void setTestData(Object[] data) {
        auditStrategy = (String) data[0];
    }

    public String getAuditStrategy() {
        return auditStrategy;
    }

	public List<Integer> extractRevisionNumbers(List queryResults) {
		List<Integer> result = new ArrayList<Integer>();
		for (Object queryResult : queryResults) {
			result.add(((DefaultRevisionEntity) ((Object[]) queryResult)[1]).getId());
		}
		return result;
	}
}
