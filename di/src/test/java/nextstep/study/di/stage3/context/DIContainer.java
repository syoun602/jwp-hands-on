package nextstep.study.di.stage3.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 스프링의 BeanFactory, ApplicationContext에 해당되는 클래스
 */
class DIContainer {

    private final Set<Object> beans;

    public DIContainer(final Set<Class<?>> classes) {
        this.beans = createBeans(classes);
        beans.forEach(this::setFields);
    }

    private Set<Object> createBeans(final Set<Class<?>> classes) {
        return classes.stream()
                .map(this::instantiateClass)
                .collect(Collectors.toSet());
    }

    private Object instantiateClass(final Class<?> targetClass) {
        try {
            final Constructor<?> constructor = targetClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException |
                NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void setFields(final Object bean) {
        final Class<?> aClass = bean.getClass();
        final Field[] declaredFields = aClass.getDeclaredFields();
        for (final Field field : declaredFields) {
            try {
                field.setAccessible(true);
                final Object fieldBean = getBean(field.getType());
                if (fieldBean != null) {
                    field.set(bean, fieldBean);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(final Class<T> aClass) {
        return (T) beans.stream()
                .filter(it -> it.getClass().equals(aClass) || aClass.isAssignableFrom(it.getClass()))
                .findFirst()
                .orElse(null);
    }
}
