package org.hibernate;

public interface UnsharedSessionBuilder extends SessionBuilder<UnsharedSessionBuilder> {
    /**
     * Define the tenant identifier to be associated with the opened session.
     *
     * @param tenantIdentifier The tenant identifier.
     *
     * @return {@code this}, for method chaining
     */
    UnsharedSessionBuilder tenantIdentifier(String tenantIdentifier);
}
