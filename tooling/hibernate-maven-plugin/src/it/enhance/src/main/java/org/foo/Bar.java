package org.foo;

import jakarta.persistence.Entity;

@Entity
public class Bar {

    private String foo;

    String getFoo() {
        return foo;
    }

    public void setFoo(String f) {
        foo = f;
    }

}
