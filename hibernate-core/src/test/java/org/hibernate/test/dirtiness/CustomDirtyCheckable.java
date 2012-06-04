package org.hibernate.test.dirtiness;

import java.util.Map;

public interface CustomDirtyCheckable {
	public Map<String, Object> getChangedValues();
}
