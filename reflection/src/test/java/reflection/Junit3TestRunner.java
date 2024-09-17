package reflection;

import org.junit.jupiter.api.Test;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class Junit3TestRunner {

    @Test
    void run() throws Exception {
        // Junit3Test에서 test로 시작하는 메소드 실행
        Class<Junit3Test> clazz = Junit3Test.class;
        final Junit3Test instance = clazz.getDeclaredConstructor().newInstance();

        final Method[] methods = clazz.getDeclaredMethods();
        final List<Method> testMethods = Arrays.stream(methods)
                .filter(method -> method.getName().startsWith("test"))
                .collect(Collectors.toList());

        testMethods.forEach(method -> invoke(method, instance));
    }

    private void invoke(final Method method, final Junit3Test instance) {
        try {
            method.invoke(instance);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
