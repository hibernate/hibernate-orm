package org.hibernate.dialect;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

public class H2DialectTestCase extends BaseNonConfigCoreFunctionalTestCase {

  private static final String H2_CONNECTION_URL_WITHOUT_DATABASE_TO_UPPER = "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
  private static final String UPDATE_ACTION = "update";

  @Override
  protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
    super.configureStandardServiceRegistryBuilder( ssrb );
    ssrb.applySetting( AvailableSettings.URL, H2_CONNECTION_URL_WITHOUT_DATABASE_TO_UPPER );
    ssrb.applySetting( AvailableSettings.HBM2DDL_AUTO, UPDATE_ACTION );
  }

  @Test
  @TestForIssue( jiraKey = "HHH-13597" )
  public void hibernateShouldStartUpWithH2AutoUpdateAndDatabaseToUpperFalse() {
    // Intentionally empty.
    // Tests if the DatabaseInformation can be correctly built during the setup.
  }
}
