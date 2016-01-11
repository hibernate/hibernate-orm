public class HibernateUtil {

    private SessionFactory sf;

    public Session getSession() {
        return getSessionFactory().openSession();
    }

    private SessionFactory getSessionFactory() {
        if ( sf == null ) {
            Bundle thisBundle = FrameworkUtil.getBundle( HibernateUtil.class );
            BundleContext context = thisBundle.getBundleContext();

            ServiceReference sr = context.getServiceReference( SessionFactory.class.getName() );
            sf = ( SessionFactory ) context.getService( sr );
        }
        return sf;
    }
}