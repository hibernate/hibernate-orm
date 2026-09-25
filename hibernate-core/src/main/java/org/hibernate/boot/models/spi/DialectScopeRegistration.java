package org.hibernate.boot.models.spi;

import org.hibernate.Remove;

@Remove
public record DialectScopeRegistration(String name, String content, String minimumVersion, String maximumVersion) {
}
