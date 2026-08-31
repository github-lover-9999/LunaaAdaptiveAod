package dev.lunaa.aod;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import org.junit.Test;

public class RuntimeFieldResolverTest {
    private static final class Dep {}

    private static final class Preferred {
        final Dep mHandler = new Dep();
        final Dep other = new Dep();
    }

    private static final class UniqueFallback {
        final String renamed = "unrelated";
        final Dep onlyDependency = new Dep();
    }

    private static final class AmbiguousFallback {
        final Dep first = new Dep();
        final Dep second = new Dep();
    }

    private static final class WrongPreferredButUniqueTypedFallback {
        final String mHandler = "wrong-type";
        final Dep renamedHandler = new Dep();
    }

    private Object resolve(Object instance, String preferred, Class<?> type) throws Exception {
        Class<?> resolver = Class.forName("dev.lunaa.aod.RuntimeFieldResolver");
        Method method = resolver.getDeclaredMethod(
                "readExactOrUniqueAssignable", Object.class, String.class, Class.class);
        method.setAccessible(true);
        return method.invoke(null, instance, preferred, type);
    }

    @Test public void exactPreferredFieldWinsEvenWhenAnotherCompatibleFieldExists() throws Exception {
        Preferred value = new Preferred();
        assertTrue(value.mHandler == resolve(value, "mHandler", Dep.class));
    }

    @Test public void uniqueCompatibleFallbackSupportsRenamedRomFields() throws Exception {
        UniqueFallback value = new UniqueFallback();
        assertTrue(value.onlyDependency == resolve(value, "mHandler", Dep.class));
    }

    @Test public void ambiguousCompatibleFallbackFailsClosed() throws Exception {
        assertTrue(resolve(new AmbiguousFallback(), "mHandler", Dep.class) == null);
    }

    @Test public void wrongTypePreferredCanUseExactlyOneTypedFallback() throws Exception {
        WrongPreferredButUniqueTypedFallback value = new WrongPreferredButUniqueTypedFallback();
        assertTrue(value.renamedHandler == resolve(value, "mHandler", Dep.class));
    }

    @Test public void nullOrMissingDependenciesFailClosed() throws Exception {
        assertTrue(resolve(null, "mHandler", Dep.class) == null);
        assertTrue(resolve(new Object(), "mHandler", Dep.class) == null);
    }
}
