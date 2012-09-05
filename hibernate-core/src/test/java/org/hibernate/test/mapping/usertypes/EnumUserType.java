package org.hibernate.test.mapping.usertypes;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;
 
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
 
public class EnumUserType implements UserType, ParameterizedType {
   
   private Class clazz = null;
   
   public static EnumUserType createInstance(Class clazz){
	   if (!clazz.isEnum())
		   throw new IllegalArgumentException("Parameter has to be an enum-class");
	   EnumUserType that=new EnumUserType();
	   Properties p=new Properties();
	   p.setProperty("enumClassName", clazz.getName());
	   that.setParameterValues(p);
	   return that;
   }
   
   public void setParameterValues(Properties params) {
      String enumClassName = params.getProperty("enumClassName");
      if (enumClassName == null) {
         throw new MappingException("enumClassName parameter not specified");
      }
      
      try {
            this.clazz = Class.forName(enumClassName);
      } catch (ClassNotFoundException e) {
    	  throw new MappingException("enumClass " + enumClassName + " not found", e);
      }
      if (!this.clazz.isEnum()){
    	  throw new MappingException("enumClass "+enumClassName+" doesn't refer to an Enum");
      }
   }
   
    private static final int[] SQL_TYPES = {Types.CHAR};
    public int[] sqlTypes() {
        return SQL_TYPES;
    }
 
    public Class returnedClass() {
        return clazz;
    }
 
    public Object nullSafeGet(ResultSet resultSet, String[] names, Object owner)
                             throws HibernateException, SQLException {
        String name = resultSet.getString(names[0]);
        Object result = null;
        if (!resultSet.wasNull()) {
            result = Enum.valueOf(clazz, name.trim());
        }
        return result;
    }
 
   @Override
	public Object nullSafeGet(ResultSet resultSet, String[] names,
			SessionImplementor session, Object owner) throws HibernateException,
			SQLException {
		return nullSafeGet(resultSet, names, owner);
	}

public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index) 
                          throws HibernateException, SQLException {
        if (null == value) {
            preparedStatement.setNull(index, Types.VARCHAR);
        } else {
            preparedStatement.setString(index, ((Enum)value).name());
        }
    }
 
    @Override
public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index,
		SessionImplementor session) throws HibernateException, SQLException {
    	nullSafeSet(preparedStatement, value, index);
}

	public Object deepCopy(Object value) throws HibernateException{
        return value;
    }
 
    public boolean isMutable() {
        return false;
    }
 
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }
 
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable)value;
    }
 
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == y)
            return true;
        if (null == x || null == y)
            return false;
        return x.equals(y);
    }
}

