package org.hibernate.engine.transaction.jta.platform.internal;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

/**
 * @author shijianliang   shijl@tongtech.com
 */
public class TongWebJtaPlatform extends AbstractJtaPlatform {
    public static final String TM_NAME = "java:comp/TransactionManager";
    public static final String UT_NAME = "java:comp/UserTransaction";

    @Override
    protected TransactionManager locateTransactionManager() {
        return (TransactionManager) jndiService().locate(TM_NAME);

    }

    @Override
    protected UserTransaction locateUserTransaction() {
        return (UserTransaction) jndiService().locate(UT_NAME);
    }
}