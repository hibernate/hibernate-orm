package org.hibernate.cfg.spi;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.cfg.PropertyData;

import java.util.*;

/**
 * 用于数据表字段排序支持，使用者可以自行定义并指定排序规则优先顺序
 * @author nierjia
 */
public interface PropertyDataSorter extends Comparable<PropertyDataSorter>{

    static PropertyDataSorter findByXClass(XClass xclass) {
         return  listBySpi(PropertyDataSorter.class)
                  .stream()
                  .filter(e->e.support(xclass))
                  .sorted(PropertyDataSorter::compareTo)
                  .findFirst()
                  .orElse(new DefPropertyDataSorter())
                  ;

    }

    boolean support(XClass clazz);

    List<PropertyData> sort(List<PropertyData> elements);

    public static <T> List<T> listBySpi(Class<T> clazz) {
        Iterator<T> it = ServiceLoader.load(clazz, clazz.getClassLoader()).iterator();
        List<T> rlist=new ArrayList<>();
        if(it!=null) {
            while(it.hasNext()){
                try {
                    T t = it.next();
                    rlist.add(t);
                } catch (ServiceConfigurationError e) {
                }
            }
        }
        return rlist;
    }
}
