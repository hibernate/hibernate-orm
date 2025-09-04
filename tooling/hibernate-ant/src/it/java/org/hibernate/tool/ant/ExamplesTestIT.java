package org.hibernate.tool.ant;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.net.URL;

public class ExamplesTestIT {

    @Test
    public void testSomethin() {
        URL url = getClass().getClassLoader().getResource("5-minute-tutorial/build.xml");
        System.out.println(url);
    }

}
