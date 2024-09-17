package reflection;

import org.junit.jupiter.api.Test;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class Junit4TestRunner {

    @Test
    void run() throws Exception {
        // Junit4Test에서 @MyTest 애노테이션이 있는 메소드 실행
        final Class<Junit4Test> clazz = Junit4Test.class;
        final Junit4Test instance = clazz.getConstructor().newInstance();
        final List<Method> methods = Arrays.stream(clazz.getDeclaredMethods())
                .filter(this::isAnnotatedWithMyTest)
                .collect(Collectors.toList());

        methods.forEach(method -> invoke(method, instance));
    }

    private boolean isAnnotatedWithMyTest(final Method method) {
        return Arrays.stream(method.getDeclaredAnnotations())
                .anyMatch(it -> it.annotationType().equals(MyTest.class));
    }

    private void invoke(final Method method, final Junit4Test instance) {
        try {
            method.invoke(instance);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
