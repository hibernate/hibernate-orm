// BMT idiom
Session sess = factory.openSession();
Transaction tx = null;
try {
    tx = sess.beginTransaction();

    // do some work
    ...

        tx.commit();
}

catch (RuntimeException e) {
    if (tx != null) tx.rollback();
    throw e; // or display error message
}

finally {
    sess.close();
}