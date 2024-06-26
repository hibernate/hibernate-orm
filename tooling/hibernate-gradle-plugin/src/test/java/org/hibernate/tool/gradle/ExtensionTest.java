package org.hibernate.tool.gradle;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class ExtensionTest {
	
	@Test
	void testExtension() {
		Map<String, Field> extensionFieldMap = new HashMap<String, Field>();
		for(Field field : Extension.class.getDeclaredFields()) {
			extensionFieldMap.put(field.getName(), field);
		}
		assertNotNull(extensionFieldMap.get("sqlToRun"));
	}

}
