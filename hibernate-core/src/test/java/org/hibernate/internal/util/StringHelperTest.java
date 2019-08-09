package org.hibernate.internal.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringHelperTest {

  @Test
  public void replaceRepeatingPlaceholdersWithoutStackOverflow() {
    String ordinalParameters = generateOrdinalParameters(3, 19999);
    String result = StringHelper.replace(
            "select * from books where category in (?1) and id in(" + ordinalParameters + ") and parent_category in (?1) and id in(" + ordinalParameters + ")",
            "?1", "?1, ?2", true, true);
    assertEquals("select * from books where category in (?1, ?2) and id in(" + ordinalParameters + ") and parent_category in (?1, ?2) and id in(" + ordinalParameters + ")", result);
  }

  private String generateOrdinalParameters(int startPosition, int endPosition) {
    StringBuilder builder = new StringBuilder();
    for (int i = startPosition; i <= endPosition; i++) {
      builder.append("?").append(i);

      if (i < endPosition) {
        builder.append(", ");
      }
    }

    return builder.toString();
  }
}
